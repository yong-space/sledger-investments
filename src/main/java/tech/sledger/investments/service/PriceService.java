package tech.sledger.investments.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private List<Position> positions = List.of(
        new Position(38442, 200_000),
        new Position(1800003, 32_000),
        new Position(8301033, 10_000),
        new Position(8090883, 20)
    );

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

    @GetMapping("/portfolio")
    public List<PortfolioEntry> getPortfolio(HttpServletResponse response) throws IOException {
        if (!saxoClient.isInit()) {
            response.sendRedirect("/authorize");
            return null;
        }
        Map<Integer, Instrument> prices = getPrices().stream()
            .collect(Collectors.toMap(Instrument::getIdentifier, i -> i));

        return positions.stream().map(position -> {
            int id = position.id();
            Instrument instrument = prices.get(id);

            // TODO: calc profit
            float profit = 0;

            return new PortfolioEntry(
                instrument.getName(),
                instrument.getPrice(),
                position.position(),
                profit
            );
        }).toList();
    }
}
