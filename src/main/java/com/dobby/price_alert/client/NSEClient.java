package com.dobby.price_alert.client;

import com.dobby.price_alert.dto.nse.FiiDataDto;
import com.dobby.price_alert.dto.nse.IndexData;
import com.dobby.price_alert.dto.nse.IndexResponseData;
import com.dobby.price_alert.dto.nse.NseResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Data
@Component
public class NSEClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${nse.base-url}")
    private String baseUrl;

    @Value("${nse.index.baseurl}")
    private String baseIndexUrl;

    private String baseFiiUrl = "https://www.nseindia.com/api/fiidiiTradeReact";

    private volatile boolean sessionWarm = false;

    public NSEClient(RestClient nseRestClient) {
        this.restClient = nseRestClient;
    }

    /**
     * Visits the homepage so the cookie jar in WebClientConfig picks up a valid session.
     */
    public synchronized void warmSession() {
        try {
            restClient.get()
                    .uri("https://www.nseindia.com/")
                    .retrieve()
                    .toBodilessEntity();
            sessionWarm = true;
            log.info("NSE session warmed");
        } catch (Exception e) {
            sessionWarm = false;
//            log.warn("Failed to warm NSE session: {}", e.getMessage());
        }
    }

    public boolean isSessionWarm() {
        return sessionWarm;
    }

    public NseResponse getPriceDetails(String symbol,String series) {
        if (!isSessionWarm()) {
            warmSession();
        }

        URI uri = UriComponentsBuilder
                .fromUriString(baseUrl)
                .queryParam("symbol", symbol)
                .queryParam("series", series)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        String referer = UriComponentsBuilder
                .fromUriString("https://www.nseindia.com/get-quotes/equity")
                .queryParam("symbol", symbol)
                .queryParam("series", series)
                .build()
                .encode()
                .toUriString();

        log.debug("Fetching NSE data for {} from {}", symbol, uri);

        String body = getWithSessionRetry(uri, referer);

        if (body == null || body.isBlank()) {
            throw new RuntimeException("Empty response from NSE");
        }
        try {
            return objectMapper.readValue(body, NseResponse.class);
        } catch (Exception e) {
            log.error("Could not get the quote data {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Bad JSON from NSE for symbol " + symbol, e);
        }
    }

    /**
     * GETs a URI with the given Referer header. If NSE rejects the request with 401/403
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
                log.info("NSE session stale (HTTP {}), re-warming and retrying", status.value());
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
    public IndexResponseData getIndexData(){
        if(!sessionWarm){
            warmSession();
        }
        URI  uri = UriComponentsBuilder.fromUriString(baseIndexUrl).build().toUri();

        String refer = UriComponentsBuilder.fromUriString("https://www.nseindia.com/api/allIndices").build().encode().toUriString();

        String body = getWithSessionRetry(uri, refer);
        if (body == null || body.isBlank()) {
            throw new RuntimeException("Empty response from NSE for Market Index Data");
        }
        try{
            return objectMapper.readValue(body, IndexResponseData.class);
        }catch (Exception e){
            log.error("Could not get the index data {}: {}", body, e.getMessage());
            throw new RuntimeException("Bad JSON from NSE for Market Index Data", e);
        }

    }
    public List<FiiDataDto> getFiiData(){
        if(!sessionWarm){
            warmSession();
        }
        URI uri = UriComponentsBuilder.fromUriString(baseFiiUrl).build().toUri();
        String refer = UriComponentsBuilder.fromUriString("https://www.nseindia.com/reports/fii-dii").build().encode().toUriString();
        String body = getWithSessionRetry(uri, refer);
        if (body == null || body.isBlank()) {
            throw new RuntimeException("Empty response from NSE for Fii Dii");
        }
        try{
            return objectMapper.readValue(body, new TypeReference<List<FiiDataDto>>() {});
        } catch (Exception e){
            log.error("Could not get the Fii Dii data {}: {}", body, e.getMessage());
            throw new RuntimeException("Bad JSON from NSE for Fii Dii", e);
        }


    }
}