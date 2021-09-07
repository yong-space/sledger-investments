package tech.sledger.investments.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tech.sledger.investments.model.PriceResponse;
import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SaxoClient {
    @Value("${saxo.uri}")
    private String saxoUri;
    @Value("${saxo.token}")
    private String saxoToken;
    private HttpHeaders headers;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Bearer " + saxoToken);
    }

    public void setAccessToken(String accessToken) {
        headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Bearer " + accessToken);
    }

    public PriceResponse getPrices(List<Integer> identifiers) {
        String idString = identifiers.stream().map(Object::toString).collect(Collectors.joining(","));
        return get("/trade/v1/infoprices/list/?AssetType=Stock&Uics=" + idString, PriceResponse.class);
    }

    public String searchInstruments(String query) {
        return get("/ref/v1/instruments/?AssetType=Stock&Keywords=" + query, String.class);
    }

    @Retryable
    private <T> T get(String uri, Class<T> responseType) {
        if (headers.get("Authorization") == null) {
            return null;
        }
        HttpEntity<Object> request = new HttpEntity<>(headers);
        return restTemplate.exchange(saxoUri + uri, HttpMethod.GET, request, responseType).getBody();
    }
}
