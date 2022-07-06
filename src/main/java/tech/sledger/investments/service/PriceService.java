package tech.sledger.investments.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.sledger.investments.client.SaxoClient;
import tech.sledger.investments.model.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Lazy(false)
@RestController
@RequiredArgsConstructor
public class PriceService {
    private final SaxoClient saxoClient;
    private final List<Position> positions = List.of(
        new Position(38442, 200_000, 0.16f, 69.35f, 1),     // Lippo
        new Position(1800003, 32_000, 0.635f, 28.94f, 1),   // IREIT
        new Position(8301033, 10_000, 1, 9, 1.35f),         // KepPacOak
        new Position(8090883, 20, 290.005f, 2.28f, 1.3581f) // SEA
    );

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
        List<Integer> instrumentIds = new ArrayList<>(positions.stream().map(Position::id).toList());
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

    private float calculateAmount(float price, float quantity, float fees, float fx) {
        return ((price * quantity) + fees) * fx;
    }

    @GetMapping("/portfolio")
    public List<PortfolioEntry> getPortfolio() {
        Map<Integer, Instrument> prices = getPrices().stream()
            .collect(Collectors.toMap(Instrument::getIdentifier, i -> i));

        Map<String, Float> fxRates = Map.of(
            "SGD", 1f,
            "USD", prices.get(45).getPrice()
        );

        return positions.stream().map(p -> {
            Instrument i = prices.get(p.id());
            float buyAmount = calculateAmount(p.buyPrice(), p.position(), p.buyFees(), p.buyFx());
            float sellAmount = calculateAmount(i.getPrice(), p.position(), p.buyFees(), fxRates.get(i.getCurrency()));
            float profit = sellAmount - buyAmount;

            return new PortfolioEntry(
                i.getSymbol(),
                i.getName(),
                i.getPrice(),
                p.position(),
                profit
            );
        }).toList();
    }
}
