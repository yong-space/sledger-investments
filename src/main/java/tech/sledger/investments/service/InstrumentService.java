package tech.sledger.investments.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.investments.client.SaxoClient;
import tech.sledger.investments.model.Instrument;
import tech.sledger.investments.model.saxo.AssetType;
import tech.sledger.investments.model.saxo.PriceEntry;
import tech.sledger.investments.model.saxo.RawInstrument;
import tech.sledger.investments.repository.InstrumentRepo;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class InstrumentService {
    private final SaxoClient saxoClient;
    private final InstrumentRepo instrumentRepo;

    public Instrument getInstrument(int instrumentId) {
        Instrument instrument = instrumentRepo.findById(instrumentId).orElse(null);
        if (instrument == null) {
            RawInstrument rawInstrument = saxoClient.searchInstruments(List.of(instrumentId)).Data()
                .stream().filter(i -> i.Identifier() == instrumentId).findFirst().orElseThrow(() -> new ResponseStatusException(BAD_REQUEST));
            PriceEntry priceEntry = saxoClient.getPrices(AssetType.valueOf(rawInstrument.AssetType()), List.of(instrumentId)).getData()
                .stream().filter(i -> i.getIdentifier() == instrumentId).findFirst().orElseThrow(() -> new ResponseStatusException(BAD_REQUEST));
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

    public Instrument getInstrument(String instrumentName) {
        Instrument i = instrumentRepo.findFirstByOrderByIdDesc();
        int nextId = (i == null) ? 1 : i.getId() + 1;
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

    public Map<Integer, Instrument> getInstrumentMap() {
        return instrumentRepo.findAll().stream().collect(Collectors.toMap(Instrument::getId, i -> i));
    }

    public void saveAll(List<Instrument> instruments) {
        instrumentRepo.saveAll(instruments);
    }
}
