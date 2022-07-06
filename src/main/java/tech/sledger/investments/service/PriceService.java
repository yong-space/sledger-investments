package tech.sledger.investments.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.sledger.investments.client.SaxoClient;
import tech.sledger.investments.model.Instrument;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Lazy(false)
@RestController
@RequiredArgsConstructor
public class PriceService {
    private final SaxoClient saxoClient;
    private final String positions = """
        38442|D51U:xses|Lippo Malls Indonesia Retail Trust
        1800003|UD1U:xses|IREIT Global
        8301033|CMOU:xses|Keppel Pacific Oak US REIT
        8090883|SE:xnys|Sea Ltd
    """;
    private List<Instrument> instruments = new ArrayList<>();

    @PostConstruct
    public void init() {
        instruments = positions.lines()
            .map(line -> {
                String[] parts = line.split("\\|");
                return Instrument.builder().identifier(Integer.parseInt(parts[0].trim())).symbol(parts[1]).name(parts[2]).build();
            })
            .collect(Collectors.toList());
    }

    @GetMapping("/search")
    public String searchInstruments(@RequestParam String query) {
        return saxoClient.searchInstruments(query);
    }

    @GetMapping("/prices")
    public List<Instrument> getPrices(HttpServletResponse response) throws IOException {
        if (!saxoClient.isInit()) {
            response.sendRedirect("/authorize");
            return null;
        }
        Map<Integer, Instrument> instrumentMap = instruments.stream()
            .collect(Collectors.toMap(Instrument::getIdentifier, i -> i));
        return saxoClient.getPrices(new ArrayList<>(instrumentMap.keySet())).getData().stream()
            .map(e -> {
                Instrument instrument = instrumentMap.get(e.getIdentifier());
                instrument.setPrice(e.getQuote().getMid());
                return instrument;
            })
            .collect(Collectors.toList());
    }
}
