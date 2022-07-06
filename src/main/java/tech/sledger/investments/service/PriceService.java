package tech.sledger.investments.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.sledger.investments.client.SaxoClient;
import tech.sledger.investments.model.Instrument;
import tech.sledger.investments.model.PortfolioEntry;
import tech.sledger.investments.model.Position;
import tech.sledger.investments.model.saxo.AssetType;
import tech.sledger.investments.model.saxo.PriceEntry;
import tech.sledger.investments.model.saxo.SearchResults;
import tech.sledger.investments.repository.InstrumentRepo;
import tech.sledger.investments.repository.PositionRepo;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static java.util.stream.Collectors.groupingBy;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PriceService {
    private final SaxoClient saxoClient;
    private final PositionRepo positionRepo;
    private final InstrumentRepo instrumentRepo;

    @GetMapping("/")
    public void home(HttpServletResponse response) throws IOException {
        String nextUrl = saxoClient.isNotAuthenticated() ? "/authorize" : "/portfolio.html";
        response.sendRedirect(nextUrl);
    }

    @GetMapping("/search")
    public SearchResults searchInstruments(
        @RequestParam(required=false) String query,
        @RequestParam(required=false) String id
    ) {
        if (id != null) {
            List<Integer> ids = Stream.of(id.split(",")).map(Integer::parseInt).toList();
            return saxoClient.searchInstruments(ids);
        }
        return saxoClient.searchInstruments(query);
    }

    @GetMapping("/prices")
    public List<Instrument> getPrices() {
        List<Integer> instrumentIds = new ArrayList<>(positionRepo.findAll().stream().map(p -> p.getInstrument().getId()).toList());
        instrumentIds.add(45);

        AtomicInteger counter = new AtomicInteger();
        Map<Integer, Instrument> instrumentMap = instrumentRepo.findAll().stream()
            .collect(Collectors.toMap(Instrument::getId, i -> i));

        counter.set(0);
        List<PriceEntry> prices = new ArrayList<>();
        instrumentIds.stream().collect(groupingBy(x -> (counter.getAndIncrement()) / 50))
            .values()
            .forEach(batch -> prices.addAll(saxoClient.getPrices(AssetType.Stock, batch).getData()));
        prices.addAll(saxoClient.getPrices(AssetType.FxSpot, List.of(45)).getData());

        List<Instrument> instruments = prices.stream()
            .map(e -> {
                BigDecimal price = e.getPriceInfoDetails().getLastTraded();
                price = price.compareTo(BigDecimal.ZERO) > 0 ? price : e.getQuote().getAsk();
                Instrument instrument = instrumentMap.get(e.getIdentifier());
                instrument.setPrice(price);
                instrument.setChange(e.getPriceInfo().getNetChange());
                instrument.setChangePercent(e.getPriceInfo().getPercentChange());
                return instrument;
            }).toList();
        instrumentRepo.saveAll(instruments);
        return instruments;
    }

    private BigDecimal calculateAmount(BigDecimal price, int quantity, BigDecimal fees, BigDecimal fx) {
        return price.multiply(BigDecimal.valueOf(quantity)).add(fees).multiply(fx);
    }

    private PortfolioEntry buildPortfolioEntry(Position position, Instrument instrument, BigDecimal fx) {
        BigDecimal buyAmount = calculateAmount(position.getBuyPrice(), position.getPosition(), position.getBuyFees(), position.getBuyFx());
        BigDecimal sellAmount = calculateAmount(instrument.getPrice(), position.getPosition(), position.getBuyFees(), fx);
        BigDecimal profit = sellAmount.subtract(buyAmount).add(position.getDividends().multiply(fx).setScale(2, RoundingMode.HALF_UP));
        BigDecimal profitPercentage = profit.multiply(BigDecimal.valueOf(100)).divide(buyAmount, 2, RoundingMode.HALF_UP);
        BigDecimal changeToday = instrument.getChange().multiply(BigDecimal.valueOf(position.getPosition())).multiply(fx);

        return PortfolioEntry.builder()
            .symbol(instrument.getSymbol())
            .name(instrument.getName())
            .position(position.getPosition())
            .price(instrument.getPrice())
            .amount(buyAmount)
            .dividends(position.getDividends().setScale(2, RoundingMode.HALF_UP))
            .changeToday(changeToday)
            .changeTodayPercentage(instrument.getChangePercent())
            .profit(profit)
            .profitPercentage(profitPercentage)
            .build();
    }

    @GetMapping("/portfolio")
    public List<PortfolioEntry> getPortfolio() {
        Map<Integer, Instrument> prices = getPrices().stream()
            .collect(Collectors.toMap(Instrument::getId, i -> i));

        Map<String, BigDecimal> fxRates = Map.of(
            "SGD", BigDecimal.valueOf(1),
            "HKD", BigDecimal.valueOf(1),
            "USD", prices.get(45).getPrice()
        );

        return positionRepo.findAll().stream().map(p -> {
            Instrument instrument = prices.get(p.getInstrument().getId());
            BigDecimal fx = fxRates.get(instrument.getCurrency());
            return buildPortfolioEntry(p, instrument, fx);
        }).toList();
    }
}
