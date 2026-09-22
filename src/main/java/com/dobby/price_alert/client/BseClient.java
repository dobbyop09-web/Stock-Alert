package com.dobby.price_alert.client;

import com.dobby.price_alert.dto.bse.BseQuoteResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Slf4j
@Data
@Component
public class BseClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${bse.api.url}")
    private String bseApiUrl;

    private volatile boolean sessionWarm = false;

    public BseClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public synchronized void warmSession() {
        try {
            restClient.get()
                    .uri("https://www.bseindia.com/")
                    .retrieve()
                    .toBodilessEntity();
            sessionWarm = true;
            log.info("BSE session warmed");
        } catch (Exception e) {
            sessionWarm = false;
//            log.warn("Failed to warm BSE session: {}", e.getMessage());
        }
    }

    public boolean isSessionWarm() {
        return sessionWarm;
    }

    public BseQuoteResponse getBseData(String scripcode) {
        if (!isSessionWarm()) {
            warmSession();
        }

        URI uri = UriComponentsBuilder
                .fromUriString(bseApiUrl)
                .queryParam("scripcode", scripcode)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        String referer = UriComponentsBuilder
                .fromUriString("https://www.bseindia.com/")
                .build()
                .encode()
                .toUriString();



        String body = getWithSessionRetry(uri, referer);
        log.info("Fetching BSE data for {} from {}", scripcode, body);
        if (body == null || body.isBlank()) {
            throw new RuntimeException("Empty response from BSE");
        }
        try {
            return objectMapper.readValue(body, BseQuoteResponse.class);
        } catch (Exception e) {
            log.error("Could not get the quote data {}: {}", scripcode, e.getMessage());
            throw new RuntimeException("Bad JSON from BSE for scripcode " + scripcode, e);
        }
    }

    /**
     * GETs a URI with the given Referer header. If BSE rejects the request with 401/403
     * (stale/missing session cookies), re-warms the session once and retries.
     */
    private String getWithSessionRetry(URI uri, String referer) {
        try {
            return restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.REFERER, referer)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 401 || status.value() == 403) {
                log.info("BSE session stale (HTTP {}), re-warming and retrying", status.value());
                sessionWarm = false;
                warmSession();
                return restClient.get()
                        .uri(uri)
                        .header(HttpHeaders.REFERER, referer)
                        .retrieve()
                        .body(String.class);
            }
            throw e;
        }
    }
}