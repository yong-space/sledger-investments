package tech.sledger.investments.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import tech.sledger.investments.client.SaxoClient;
import tech.sledger.investments.model.Instrument;
import tech.sledger.investments.model.PriceResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TokenService {
    @Value("${saxo.auth-uri}")
    private String saxoAuthUri;
    @Value("${saxo.app-key}")
    private String saxoAppKey;
    @Value("${saxo.app-secret}")
    private String saxoAppSecret;
    @Value("${saxo.redirect-uri}")
    private String saxoRedirectUri;
    private final SaxoClient saxoClient;
    private final ObjectMapper objectMapper;
    private String refreshToken;

    @GetMapping("/authorize")
    public void authorize(HttpServletResponse response) throws IOException {
        String authUri = saxoAuthUri
            .concat("/authorize?response_type=code&client_id=")
            .concat(saxoAppKey)
            .concat("&redirect_uri=")
            .concat(saxoRedirectUri);
        response.sendRedirect(authUri);
    }

    @GetMapping("/token")
    public String token(
        @RequestParam String code,
        @RequestParam(required=false) String error,
        @RequestParam(required=false, name="error_description") String errorDescription
    ) {
        if (error != null) {
            return error + ": " + errorDescription;
        }
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("grant_type", "authorization_code");
        data.add("code", code);
        data.add("redirect_uri", saxoRedirectUri);
        data.add("client_id", saxoAppKey);
        data.add("client_secret", saxoAppSecret);
        String accessToken = getNewToken(data);
        return "Hello " + accessToken;
    }

    @GetMapping("/refresh")
    public String refresh() {
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("grant_type", "refresh_token");
        data.add("refresh_token", refreshToken);
        data.add("redirect_uri", saxoRedirectUri);
        String accessToken = getNewToken(data);
        return "Hello " + accessToken;
    }

    private String getNewToken(MultiValueMap<String, String> data) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String response = restTemplate.postForObject(saxoAuthUri + "/token", new HttpEntity<>(data, headers), String.class);
        JsonNode responseNode;
        try {
            responseNode = objectMapper.readTree(response);
        } catch (JsonProcessingException e) {
            return null;
        }
        String accessToken = responseNode.path("access_token").asText();
        refreshToken = responseNode.path("refresh_token").asText();
        int refreshTokenExpiresIn = responseNode.path("refresh_token_expires_in").asInt();
        // TODO trigger timer
        saxoClient.setAccessToken(accessToken);
        return accessToken;
    }

    @GetMapping("/search")
    public String searchInstruments(@RequestParam String query) {
        return saxoClient.searchInstruments(query);
    }

    @GetMapping("/prices")
    public List<Instrument> getPrices() {
        List<Instrument> instruments = List.of(
            Instrument.builder().identifier(38442).symbol("D51U:xses").name("Lippo Malls Indonesia Retail Trust").build(),
            Instrument.builder().identifier(1800003).symbol("UD1U:xses").name("IREIT Global").build(),
            Instrument.builder().identifier(8301033).symbol("CMOU:xses").name("Keppel Pacific Oak US REIT").build(),
            Instrument.builder().identifier(8090883).symbol("SE:xnys").name("Sea Ltd").build()
        );
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
