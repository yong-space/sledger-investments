package tech.sledger.investments.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tech.sledger.investments.model.PriceResponse;
import tech.sledger.investments.model.SaxoAssetType;
import tech.sledger.investments.model.SaxoSearchResults;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SaxoClient {
    @Value("${saxo.uri}")
    private String saxoUri;
    private HttpHeaders headers = null;
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isNotAuthenticated() {
        return headers == null;
    }

    public void setAccessToken(String accessToken) {
        headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Bearer " + accessToken);
    }

    @Cacheable(value = "prices", unless = "#result==null")
    public PriceResponse getPrices(SaxoAssetType assetType, List<Integer> identifiers) {
        String idString = identifiers.stream().map(Object::toString).collect(Collectors.joining(","));
        return get("/trade/v1/infoprices/list/?AssetType=" + assetType + "&Uics=" + idString, PriceResponse.class);
    }

    public SaxoSearchResults searchInstruments(String query) {
        return get("/ref/v1/instruments/?AssetType=FxSpot&Keywords=" + query, SaxoSearchResults.class);
    }

    @Cacheable(value = "instruments", unless = "#result==null")
    public SaxoSearchResults searchInstruments(List<Integer> identifiers) {
        String idString = identifiers.toString().replaceAll(" ", "");
        return get("/ref/v1/instruments/?Uics=" + idString.substring(1, idString.length() - 1), SaxoSearchResults.class);
    }

    @Retryable
    private <T> T get(String uri, Class<T> responseType) {
        if (headers == null) {
            log.error("Not authenticated yet");
            return null;
        }
        if (headers.get("Authorization") != null) {
            String auth = headers.get("Authorization").toString();
            log.info("Authorization: {}", auth.substring(8, auth.length() - 1));
        }

        HttpEntity<Object> request = new HttpEntity<>(headers);
        return restTemplate.exchange(saxoUri + uri, HttpMethod.GET, request, responseType).getBody();
    }
}
