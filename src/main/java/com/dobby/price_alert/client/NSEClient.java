package com.dobby.price_alert.client;

import com.dobby.price_alert.dto.nse.fii.FiiDataDto;
import com.dobby.price_alert.dto.nse.index.IndexResponseData;
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
import com.dobby.price_alert.dto.nse.derivatives.DerivativesResponse;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    @Value("${nse.base.futures-url}")
    private String baseFuturesUrl;

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
    public DerivativesResponse getFutureData(String symbol) {

        if (!isSessionWarm()) {
            warmSession();
        }

        URI uri = UriComponentsBuilder
                .fromUriString(baseFuturesUrl)
                .queryParam("symbol", symbol)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        String referer = UriComponentsBuilder
                .fromUriString("https://www.nseindia.com/get-quotes/derivatives")
                .build()
                .toUriString();

        log.debug("Fetching NSE futures data for {} from {}", symbol, uri);

        String body = getWithSessionRetry(uri, referer);

        if (body == null || body.isBlank()) {
            throw new RuntimeException(
                    "Empty response from NSE for futures symbol " + symbol
            );
        }

        try {
            return objectMapper.readValue(body, DerivativesResponse.class);
        } catch (Exception e) {
            log.error(
                    "Could not get futures data for {}: {}",
                    symbol,
                    e.getMessage()
            );

            throw new RuntimeException(
                    "Bad JSON from NSE for futures symbol " + symbol,
                    e
            );
        }
    }
    public String getParticipantWiseOiReport(LocalDate date) {

        if (!isSessionWarm()) {
            warmSession();
        }

        String archivesJson = """
            [{"name":"F&O - Participant wise Open Interest(csv)",
              "type":"archives",
              "category":"derivatives",
              "section":"equity"}]
            """.replaceAll("\\s+", " ");

        URI uri = UriComponentsBuilder
                .fromUriString("https://www.nseindia.com/api/reports")
                .queryParam("archives", archivesJson)
                .queryParam("date",
                        date.format(DateTimeFormatter.ofPattern(
                                "dd-MMM-yyyy", Locale.ENGLISH)))
                .queryParam("type", "equity")
                .queryParam("mode", "single")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        String referer = "https://www.nseindia.com/all-reports-derivatives";

        log.info("Downloading participant-wise OI report for {}", date);

        return getWithSessionRetry(uri, referer);
    }


    public Map<String, Object> getOptionChainContractInfo(String symbol) {

        if (!isSessionWarm()) {
            warmSession();
        }

        URI uri = UriComponentsBuilder
                .fromUriString("https://www.nseindia.com/api/option-chain-contract-info")
                .queryParam("symbol", symbol)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        String referer = "https://www.nseindia.com/option-chain";

        log.info("Fetching option chain expiry dates for {}", symbol);

        String body = getWithSessionRetry(uri, referer);

        if (body == null || body.isBlank()) {
            throw new RuntimeException("Empty NSE option chain contract info");
        }

        try {
            return objectMapper.readValue(
                    body,
                    new TypeReference<Map<String, Object>>() {}
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to parse option chain contract info for " + symbol, e
            );
        }
    }

    public Map<String, Object> getIndexOptionChain(String symbol, String expiry) {

        if (!isSessionWarm()) {
            warmSession();
        }

        URI uri = UriComponentsBuilder
                .fromUriString("https://www.nseindia.com/api/option-chain-v3")
                .queryParam("type", "Indices")
                .queryParam("symbol", symbol)
                .queryParam("expiry", expiry)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        String referer = "https://www.nseindia.com/option-chain";

        log.info("Fetching {} option chain for expiry {}", symbol, expiry);

        String body = getWithSessionRetry(uri, referer);

        if (body == null || body.isBlank()) {
            throw new RuntimeException(
                    "Empty NSE option chain response for " + symbol + " " + expiry
            );
        }

        try {
            return objectMapper.readValue(
                    body,
                    new TypeReference<Map<String, Object>>() {}
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to parse option chain for " + symbol + " " + expiry, e
            );
        }
    }



    public List<Map<String, Object>> getVixHistory(
            LocalDate fromDate,
            LocalDate toDate) {

        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException(
                    "From date cannot be after to date"
            );
        }

        if (!isSessionWarm()) {
            warmSession();
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd-MM-yyyy");

        URI uri = UriComponentsBuilder
                .fromUriString(
                        "https://www.nseindia.com/api/historicalOR/vixhistory"
                )
                .queryParam("from", fromDate.format(formatter))
                .queryParam("to", toDate.format(formatter))
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        String referer =
                "https://www.nseindia.com/reports-indices-historical-vix";

        log.info(
                "Fetching NSE VIX history from {} to {}",
                fromDate, toDate
        );

        String body = getWithSessionRetry(uri, referer);

        if (body == null || body.isBlank()) {
            throw new RuntimeException("Empty NSE VIX history response");
        }

        try {

            // Parse the response without assuming its root structure
            Object parsed = objectMapper.readValue(body, Object.class);

            Object rows = parsed;

            // Support {"data": [...]}
            if (parsed instanceof Map<?, ?> responseMap) {

                rows = responseMap.get("data");

                // Also support {"records": {"data": [...]}}
                if (!(rows instanceof List<?>)) {
                    Object records = responseMap.get("records");

                    if (records instanceof Map<?, ?> recordsMap) {
                        rows = recordsMap.get("data");
                    }
                }
            }

            if (!(rows instanceof List<?> list)) {
                throw new IllegalStateException(
                        "Unexpected NSE VIX response structure. Expected JSON array."
                );
            }

            List<Map<String, Object>> result = new ArrayList<>();

            for (Object row : list) {

                if (!(row instanceof Map<?, ?> map)) {
                    throw new IllegalStateException(
                            "Unexpected VIX record format: " + row
                    );
                }

                Map<String, Object> record = new LinkedHashMap<>();

                map.forEach((key, value) ->
                        record.put(String.valueOf(key), value)
                );

                result.add(record);
            }

            log.info("Successfully parsed {} VIX records", result.size());

            return result;

        } catch (Exception e) {

            String preview = body.length() > 1500
                    ? body.substring(0, 1500)
                    : body;

            log.error(
                    "Failed to parse NSE VIX history. Cause: {}. Response preview: {}",
                    e.getMessage(),
                    preview,
                    e
            );

            throw new RuntimeException(
                    "Failed to parse NSE VIX history: " + e.getMessage(),
                    e
            );
        }
    }




}