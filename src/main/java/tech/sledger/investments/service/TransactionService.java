package tech.sledger.investments.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tech.sledger.investments.client.SaxoClient;
import tech.sledger.investments.model.Instrument;
import tech.sledger.investments.model.Transaction;
import tech.sledger.investments.model.TransactionType;
import tech.sledger.investments.model.saxo.AssetType;
import tech.sledger.investments.model.saxo.PriceEntry;
import tech.sledger.investments.model.saxo.RawInstrument;
import tech.sledger.investments.repository.InstrumentRepo;
import tech.sledger.investments.repository.TransactionRepo;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static tech.sledger.investments.model.TransactionType.Dividends;
import static tech.sledger.investments.model.TransactionType.Trade;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepo txRepo;
    private final ReconService recon;
    private final InstrumentRepo instrumentRepo;
    private final SaxoClient saxoClient;

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
            recon.reconcilePositions(); // TODO: Make this more efficient
        }

        return new Response();
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
            PriceEntry priceEntry = saxoClient.getPrices(AssetType.Stock, List.of(instrumentId)).getData().get(0);
            BigDecimal price = priceEntry.getPriceInfoDetails().getLastTraded();
            price = price.compareTo(BigDecimal.ZERO) > 0 ? price : priceEntry.getQuote().getAsk();
            instrument = Instrument.builder()
                .id(instrumentId)
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
        int quantity,
        BigDecimal notional
    ) {}
}
