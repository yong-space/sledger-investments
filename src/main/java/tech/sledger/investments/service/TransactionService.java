package tech.sledger.investments.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.investments.model.*;
import tech.sledger.investments.repository.PositionRepo;
import tech.sledger.investments.repository.TransactionRepo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static tech.sledger.investments.model.TransactionType.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TransactionService {
    private final InstrumentService instrumentService;
    private final TransactionRepo txRepo;
    private final PositionRepo positionRepo;

    @Data
    static class Response {
        String status = "ok";
    }

    @CrossOrigin
    @GetMapping("/tx")
    public List<Transaction> listTransactions() {
        return txRepo.findAllByOrderByDateDesc();
    }

    @GetMapping("/tx/{id}")
    public Transaction getTransaction(@PathVariable int id) {
        return txRepo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }

    @CrossOrigin
    @DeleteMapping("/tx/{id}")
    public Response deleteTx(@PathVariable int id) {
        Transaction tx = txRepo.findById(id).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "No such transaction"));
        txRepo.delete(tx);

        if (List.of(Deposit, Fees, Interest).contains(tx.getType())) {
            return new Response();
        }

        Instrument instrument = instrumentService.getInstrument(tx.getInstrumentId());
        WorkingPosition w = calculatePosition(txRepo.findAllByInstrumentIdOrderByDate(instrument.getId()));

        Position position = positionRepo.findFirstByInstrument(instrument);
        if (w.getLatestPosition().equals(BigDecimal.ZERO)) {
            positionRepo.delete(position);
        } else if (tx.getType() == Trade || tx.getType() == Dividends) {
            position.setPosition(w.getLatestPosition());
            position.setBuyPrice(w.getTotalAmount().divide(w.getLatestPosition(), RoundingMode.HALF_EVEN).abs());
            position.setBuyFees(w.getTotalAmount().abs().subtract(w.getTotalNotionalAmount()));
            position.setDividends(w.getDividends());
            position.setBuyFx(w.getTotalAmountLocal().divide(w.getTotalAmount(), RoundingMode.HALF_EVEN));
            positionRepo.save(position);
        }
        return new Response();
    }

    @CrossOrigin
    @PostMapping("/tx")
    public Transaction addTrade(@RequestBody NewTransaction newTx) {
        int instrumentId = 0;
        Instrument instrument;
        try {
            instrumentId = Integer.parseInt(newTx.instrument());
            instrument = instrumentService.getInstrument(instrumentId);
        } catch (NumberFormatException e) {
            instrument = instrumentService.getInstrument(newTx.instrument());
        }

        Transaction t = txRepo.findFirstByOrderByIdDesc();
        int nextId = (t == null) ? 1 : t.getId() + 1;

        Transaction.TransactionBuilder txBuilder = Transaction.builder()
            .id(nextId)
            .type(newTx.type())
            .date(newTx.date());

        txBuilder = switch (newTx.type()) {
            case Trade -> txBuilder
                .instrumentId(instrumentId)
                .ticker(newTx.ticker())
                .price(newTx.price())
                .amount(newTx.notional())
                .quantity(newTx.quantity())
                .fxRate(getFxRate(instrument.getCurrency(), newTx.date()));
            case Dividends -> txBuilder.instrumentId(instrumentId).ticker(newTx.ticker()).amount(newTx.notional());
            case Deposit -> txBuilder.price(newTx.price()).amount(newTx.notional());
            case Interest, Fees -> txBuilder.instrumentId(0).amount(newTx.notional());
        };

        Transaction transaction = txBuilder.build();
        log.info(transaction.toString());

        if (List.of(Trade, Dividends).contains(newTx.type())) {
            updatePosition(instrument, transaction);
        }

        txRepo.save(transaction);
        return transaction;
    }

    private void updatePosition(Instrument instrument, Transaction transaction) {
        Position position = positionRepo.findFirstByInstrument(instrument);
        if (position == null) {
            if (transaction.getType() == Dividends) {
                throw new ResponseStatusException(BAD_REQUEST, "Cannot add dividend without position");
            }
            Position p = positionRepo.findFirstByOrderByIdDesc();
            int id = (p == null) ? 1 : p.getId() + 1;
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
                List<Transaction> transactions = new ArrayList<>(txRepo.findAllByInstrumentIdOrderByDate(instrument.getId()));
                transactions.add(transaction);
                transactions.sort(Comparator.comparing(Transaction::getDate));
                WorkingPosition w = calculatePosition(transactions);
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

    public BigDecimal getFxRate(String currency, Instant date) {
        BigDecimal fxRate = BigDecimal.ONE;
        if (currency.equals("USD")) {
            Transaction tx = txRepo.findFirstByTypeAndDateLessThanEqualAndPriceIsNotNullOrderByDateDesc(Deposit, date);
            if (tx == null) {
                tx = txRepo.findFirstByTypeAndDateGreaterThanAndPriceIsNotNullOrderByDate(Deposit, date);
            }
            return (tx == null) ? fxRate : tx.getPrice();
        }
        return fxRate;
    }
}
