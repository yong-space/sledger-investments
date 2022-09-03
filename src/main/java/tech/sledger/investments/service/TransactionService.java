package tech.sledger.investments.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.investments.client.SaxoClient;
import tech.sledger.investments.model.*;
import tech.sledger.investments.model.saxo.AssetType;
import tech.sledger.investments.model.saxo.PriceEntry;
import tech.sledger.investments.model.saxo.RawInstrument;
import tech.sledger.investments.repository.InstrumentRepo;
import tech.sledger.investments.repository.PositionRepo;
import tech.sledger.investments.repository.TransactionRepo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import static tech.sledger.investments.model.TransactionType.Dividends;
import static tech.sledger.investments.model.TransactionType.Trade;

@Slf4j
@CrossOrigin
@RestController
@RequiredArgsConstructor
public class TransactionService {
    private final SaxoClient saxoClient;
    private final TransactionRepo txRepo;
    private final InstrumentRepo instrumentRepo;
    private final PositionRepo positionRepo;

    @Data
    static class Response {
        String status = "ok";
    }

    @PostMapping("/add-tx")
    public Response addTrade(@RequestBody NewTransaction newTx) {
        log.info(newTx.toString());

        int instrumentId = 0;
        Instrument instrument;
        try {
            instrumentId = Integer.parseInt(newTx.instrument);
            instrument = getInstrument(instrumentId);
        } catch (NumberFormatException e) {
            instrument = getInstrument(newTx.instrument);
        }

        Transaction.TransactionBuilder txBuilder = Transaction.builder()
            .id(txRepo.findFirstByOrderByIdDesc().getId() + 1)
            .type(newTx.type)
            .date(newTx.date);

        txBuilder = switch (newTx.type) {
            case Trade -> txBuilder
                .instrumentId(instrumentId)
                .ticker(newTx.ticker)
                .price(newTx.price)
                .amount(newTx.notional)
                .quantity(newTx.quantity)
                .fxRate(getFxRate(instrument.getCurrency(), newTx.date));
            case Dividends -> txBuilder.instrumentId(instrumentId).ticker(newTx.ticker).amount(newTx.notional);
            case Deposit -> txBuilder.price(newTx.price).amount(newTx.notional);
            case Interest, Fees -> txBuilder.instrumentId(0).amount(newTx.notional);
        };

        Transaction transaction = txBuilder.build();
        log.info(transaction.toString());
        txRepo.save(transaction);

        if (List.of(Trade, Dividends).contains(newTx.type)) {
            updatePosition(instrument, transaction);
        }

        return new Response();
    }

    private void updatePosition(Instrument instrument, Transaction transaction) {
        Position position = positionRepo.findFirstByInstrument(instrument);
        if (position == null) {
            if (transaction.getType() == Dividends) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot add dividend without position");
            }
            int id = positionRepo.findFirstByOrderByIdDesc().getId() + 1;
            BigDecimal notional = transaction.getPrice().multiply(transaction.getQuantity());
            position = Position.builder()
                .id(id)
                .instrument(instrument)
                .position(transaction.getQuantity())
                .buyPrice(transaction.getPrice())
                .buyFees(transaction.getAmount().subtract(notional))
                .dividends(BigDecimal.ZERO)
                .buyFx(transaction.getFxRate())
                .build();

        } else if (transaction.getType() == Trade) {
            BigDecimal newPosition = position.getPosition().add(transaction.getQuantity());
            if (newPosition.equals(BigDecimal.ZERO)) {
                positionRepo.delete(position); // Position closed
                return;
            } else {
                WorkingPosition w = calculatePosition(txRepo.findAllByInstrumentIdOrderByDate(instrument.getId()));
                position.setPosition(newPosition);
                position.setBuyPrice(w.getTotalAmount().divide(w.getLatestPosition(), RoundingMode.HALF_EVEN).abs());
                position.setBuyFees(w.getTotalAmount().abs().subtract(w.getTotalNotionalAmount()));
                position.setDividends(w.getDividends());
                position.setBuyFx(w.getTotalAmountLocal().divide(w.getTotalAmount(), RoundingMode.HALF_EVEN));
            }
        } else {
            position.setDividends(position.getDividends().add(transaction.getAmount()));
        }
        positionRepo.save(position);
    }

    public WorkingPosition calculatePosition(List<Transaction> transactions) {
        WorkingPosition w = new WorkingPosition();
        for (Transaction t : transactions) {
            switch (t.getType()) {
                case Trade -> {
                    w.setLatestPosition(w.getLatestPosition().add(t.getQuantity()));
                    if (w.getLatestPosition().equals(BigDecimal.ZERO)) {
                        w = new WorkingPosition();
                    } else {
                        w.setTotalAmount(w.getTotalAmount().add(t.getAmount()));
                        w.setTotalAmountLocal(w.getTotalAmountLocal().add(t.getAmount().multiply(t.getFxRate())));
                        w.setTotalPrice(w.getTotalPrice().add(t.getPrice()));
                        w.setTotalNotionalAmount(w.getTotalNotionalAmount().add(t.getPrice().multiply(t.getQuantity())));
                    }
                }
                case Dividends -> {
                    w.setDividends(w.getDividends().add(t.getAmount()));
                }
            }
        }
        return w;
    }

    private BigDecimal getFxRate(String currency, Instant date) {
        BigDecimal fxRate = BigDecimal.ONE;
        if (currency.equals("USD")) {
            Transaction tx = txRepo.findFirstByTypeAndDateLessThanEqualAndPriceIsNotNullOrderByDateDesc(TransactionType.Deposit, date);
            if (tx == null) {
                tx = txRepo.findFirstByTypeAndDateGreaterThanAndPriceIsNotNullOrderByDate(TransactionType.Deposit, date);
            }
            return tx.getPrice();
        }
        return fxRate;
    }

    private Instrument getInstrument(int instrumentId) {
        Instrument instrument = instrumentRepo.findById(instrumentId).orElse(null);
        if (instrument == null) {
            RawInstrument rawInstrument = saxoClient.searchInstruments(List.of(instrumentId)).Data().get(0);
            PriceEntry priceEntry = saxoClient.getPrices(AssetType.valueOf(rawInstrument.AssetType()), List.of(instrumentId)).getData().get(0);
            BigDecimal price = priceEntry.getPriceInfoDetails().getLastTraded();
            price = price.compareTo(BigDecimal.ZERO) > 0 ? price : priceEntry.getQuote().getAsk();
            instrument = Instrument.builder()
                .id(instrumentId)
                .assetType(AssetType.valueOf(rawInstrument.AssetType()))
                .name(rawInstrument.Description())
                .symbol(rawInstrument.Symbol())
                .currency(rawInstrument.CurrencyCode())
                .price(price)
                .change(priceEntry.getPriceInfo().getNetChange())
                .changePercent(priceEntry.getPriceInfo().getPercentChange())
                .build();
            instrumentRepo.save(instrument);
        }
        return instrument;
    }

    private Instrument getInstrument(String instrumentName) {
        int nextId = instrumentRepo.findFirstByOrderByIdDesc().getId() + 1;
        if (nextId < 2000000000) {
            nextId = 2000000000;
        }
        Instrument instrument = instrumentRepo.findTopByName(instrumentName);

        if (instrument == null) {
            instrument = Instrument.builder()
                .id(nextId)
                .name(instrumentName)
                .build();
            instrumentRepo.save(instrument);
        }
        return instrument;
    }

    public record NewTransaction(
        TransactionType type,
        Instant date,
        String instrument,
        String ticker,
        BigDecimal price,
        BigDecimal quantity,
        BigDecimal notional
    ) {}
}
