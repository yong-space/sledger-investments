package tech.sledger.investments.service;

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

@Slf4j
@RestController
@RequiredArgsConstructor
public class TradeService {
    private final TransactionRepo txRepo;
    private final ReconService recon;
    private final InstrumentRepo instrumentRepo;
    private final SaxoClient saxoClient;

    @PostMapping("/add-tx")
    public void addTrade(@RequestBody NewTrade newTrade) {
        log.info(newTrade.toString());

        int instrumentId = newTrade.instrument;
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

        BigDecimal fxRate = BigDecimal.ONE;
        if (instrument.getCurrency().equals("USD")) {
            fxRate = txRepo.findFirstByTypeAndDateLessThanEqualAndPriceIsNotNullOrderByDateDesc(TransactionType.Deposit, newTrade.date).getPrice();
        }
        Transaction transaction = Transaction.builder()
            .id(txRepo.findFirstByOrderByIdDesc().getId() + 1)
            .type(TransactionType.Trade)
            .date(newTrade.date)
            .instrumentId(newTrade.instrument)
            .ticker(newTrade.ticker)
            .price(newTrade.price)
            .amount(newTrade.notional)
            .quantity(newTrade.quantity)
            .fxRate(fxRate)
            .build();
        log.info(transaction.toString());
        txRepo.save(transaction);

        recon.reconcilePositions();
    }

    public record NewTrade(
        Instant date,
        int instrument,
        String ticker,
        BigDecimal price,
        int quantity,
        BigDecimal notional
    ) {}
}
