package tech.sledger.investments.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.investments.model.saxo.AssetType;
import tech.sledger.investments.model.saxo.PriceResponse;
import tech.sledger.investments.model.saxo.SearchResults;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SaxoClient {
    @Value("${saxo.uri}")
    private String saxoUri;
    private HttpHeaders headers = null;
    private final RestTemplate restTemplate = new RestTemplate();

    public void setAccessToken(String accessToken) {
        headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Bearer " + accessToken);
    }

    @Cacheable(value = "prices", unless = "#result==null")
    public PriceResponse getPrices(AssetType assetType, List<Integer> identifiers) {
        String idString = identifiers.stream().map(Object::toString).collect(Collectors.joining(","));
        return get("/trade/v1/infoprices/list/?FieldGroups=PriceInfo,PriceInfoDetails&AssetType=" + assetType + "&Uics=" + idString, PriceResponse.class);
    }

    public SearchResults searchInstruments(String query) {
        return get("/ref/v1/instruments/?AssetTypes=Stock,FxSpot&Keywords=" + query, SearchResults.class);
    }

    @Cacheable(value = "instruments", unless = "#result==null")
    public SearchResults searchInstruments(List<Integer> identifiers) {
        String idString = identifiers.toString().replaceAll(" ", "");
        return get("/ref/v1/instruments/?Uics=" + idString.substring(1, idString.length() - 1), SearchResults.class);
    }

    @Retryable
    private <T> T get(String uri, Class<T> responseType) {
        if (headers == null) {
            log.error("Not authenticated yet");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated yet");
        }
        HttpEntity<Object> request = new HttpEntity<>(headers);
        return restTemplate.exchange(saxoUri + uri, HttpMethod.GET, request, responseType).getBody();
    }
}
