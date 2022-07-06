package tech.sledger.investments.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.sledger.investments.client.SaxoClient;
import tech.sledger.investments.model.*;
import tech.sledger.investments.repository.PositionRepo;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PriceService {
    private final SaxoClient saxoClient;
    private final PositionRepo positionRepo;

    @GetMapping("/")
    public void home(HttpServletResponse response) throws IOException {
        String nextUrl = saxoClient.isNotAuthenticated() ? "/authorize" : "/portfolio.html";
        response.sendRedirect(nextUrl);
    }

    @GetMapping("/search")
    public SaxoSearchResults searchInstruments(
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
        List<Integer> instrumentIds = new ArrayList<>(positionRepo.findAll().stream().map(Position::id).toList());
        instrumentIds.add(45);
        List<SaxoInstrument> rawInstruments = saxoClient.searchInstruments(instrumentIds).Data();
        Map<Integer, Instrument> instrumentMap = rawInstruments.stream()
            .collect(Collectors.toMap(SaxoInstrument::Identifier, i -> Instrument.builder()
                .identifier(i.Identifier())
                .name(i.Description())
                .symbol(i.Symbol())
                .currency(i.CurrencyCode())
                .build()));

        List<PriceEntry> prices = new ArrayList<>(saxoClient.getPrices(SaxoAssetType.Stock, instrumentIds).getData());
        prices.addAll(saxoClient.getPrices(SaxoAssetType.FxSpot, List.of(45)).getData());

        return prices.stream()
            .map(e -> {
                Instrument instrument = instrumentMap.get(e.getIdentifier());
                instrument.setPrice(e.getQuote().getMid());
                return instrument;
            })
            .collect(Collectors.toList());
    }

    private BigDecimal calculateAmount(BigDecimal price, int quantity, BigDecimal fees, BigDecimal fx) {
        return price.multiply(BigDecimal.valueOf(quantity)).add(fees).multiply(fx);
    }

    @GetMapping("/portfolio")
    public List<PortfolioEntry> getPortfolio() {
        Map<Integer, Instrument> prices = getPrices().stream()
            .collect(Collectors.toMap(Instrument::getIdentifier, i -> i));

        Map<String, BigDecimal> fxRates = Map.of(
            "SGD", BigDecimal.valueOf(1),
            "USD", prices.get(45).getPrice()
        );

        return positionRepo.findAll().stream().map(p -> {
            Instrument i = prices.get(p.id());
            BigDecimal buyAmount = calculateAmount(p.buyPrice(), p.position(), p.buyFees(), p.buyFx());
            BigDecimal sellAmount = calculateAmount(i.getPrice(), p.position(), p.buyFees(), fxRates.get(i.getCurrency()));
            BigDecimal profit = sellAmount.subtract(buyAmount).add(p.dividends().multiply(fxRates.get(i.getCurrency()))).setScale(2, RoundingMode.HALF_UP);
            BigDecimal profitPercentage = profit.multiply(BigDecimal.valueOf(100)).divide(buyAmount, 2, RoundingMode.HALF_UP);

            return new PortfolioEntry(
                i.getSymbol(),
                i.getName(),
                p.position(),
                i.getPrice(),
                p.dividends().setScale(2, RoundingMode.HALF_UP),
                profit,
                profitPercentage
            );
        }).toList();
    }
}
