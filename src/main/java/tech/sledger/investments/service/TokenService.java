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
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tech.sledger.investments.client.SaxoClient;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TokenService {
    @Value("${saxo.auth-uri}")
    private String saxoAuthUri;
    @Value("${saxo.app-key}")
    private String saxoClientId;
    @Value("${saxo.app-secret}")
    private String saxoClientSecret;
    @Value("${saxo.redirect-uri}")
    private String saxoRedirectUri;
    private final SaxoClient saxoClient;
    private final ObjectMapper objectMapper;
    private Timer timer = new Timer(true);

    @GetMapping("/authorize")
    public void authorize(HttpServletResponse response) throws IOException {
        String authUri = saxoAuthUri
            .concat("/authorize?response_type=code&client_id=")
            .concat(saxoClientId)
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
        data.add("client_id", saxoClientId);
        data.add("client_secret", saxoClientSecret);
        String accessToken = getNewToken(data);
        return "Hello " + accessToken;
    }

    private String getNewToken(MultiValueMap<String, String> data) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(saxoClientId, saxoClientSecret));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String response;
        try {
            response = restTemplate.postForObject(saxoAuthUri + "/token", new HttpEntity<>(data, headers), String.class);
        } catch (RestClientException e) {
            return "Error: " + e.getMessage();
        }
        JsonNode responseNode;
        try {
            responseNode = objectMapper.readTree(response);
        } catch (JsonProcessingException e) {
            return null;
        }

        if (!saxoClient.isInit()) {
            int nextRefresh = responseNode.path("expires_in").asInt() * 1000 - 1000;
            String refreshToken = responseNode.path("refresh_token").asText();
            scheduleRefreshToken(refreshToken, nextRefresh);
        }

        String accessToken = responseNode.path("access_token").asText();
        saxoClient.setAccessToken(accessToken);
        return accessToken;
    }

    private void scheduleRefreshToken(String refreshToken, long interval) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                log.info("Refreshing token");
                MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
                data.add("grant_type", "refresh_token");
                data.add("refresh_token", refreshToken);
                data.add("redirect_uri", saxoRedirectUri);
                getNewToken(data);
            }
        }, interval, interval);
    }
}
