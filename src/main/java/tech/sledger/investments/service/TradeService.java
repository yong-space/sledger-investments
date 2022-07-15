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
import tech.sledger.investments.model.saxo.*;
import tech.sledger.investments.repository.InstrumentRepo;
import tech.sledger.investments.repository.TransactionRepo;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TradeService {
    private final TransactionRepo txRepo;
    private final ReconService recon;
    private final InstrumentRepo instrumentRepo;
    private final SaxoClient saxoClient;

    @PostMapping("/add-trade")
    public void addTrade(@RequestBody NewTrade newTrade) {
        log.info(newTrade.toString());
        Instant startOfToday = Instant.now().atZone(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS).toInstant();
        Transaction transaction = Transaction.builder()
            .id(txRepo.findFirstByOrderByIdDesc().getId() + 1)
            .date(startOfToday)
            .type(TransactionType.Trade)
            .instrumentId(newTrade.instrument)
            .ticker(newTrade.ticker)
            .price(newTrade.price)
            .amount(newTrade.notional)
            .quantity(newTrade.quantity)
            .fxRate(BigDecimal.ONE)
            .build();
        log.info(transaction.toString());
        txRepo.save(transaction);

        int instrumentId = newTrade.instrument;
        if (instrumentRepo.findById(instrumentId).isEmpty()) {
            RawInstrument rawInstrument = saxoClient.searchInstruments(List.of(instrumentId)).Data().get(0);
            PriceEntry priceEntry = saxoClient.getPrices(AssetType.Stock, List.of(instrumentId)).getData().get(0);
            BigDecimal price = priceEntry.getPriceInfoDetails().getLastTraded();
            price = price.compareTo(BigDecimal.ZERO) > 0 ? price : priceEntry.getQuote().getAsk();
            Instrument instrument = Instrument.builder()
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

        recon.reconcilePositions();
    }

    public record NewTrade(int instrument, String ticker, BigDecimal price, int quantity, BigDecimal notional) {}
}
