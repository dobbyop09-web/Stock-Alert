package com.dobby.price_alert.service.kotak;

import com.dobby.price_alert.config.KotakNeoConfig;
import com.dobby.price_alert.dto.kotak.KotakQuoteResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class KotakQuoteService {

    private final RestTemplate restTemplate;
    private final KotakNeoConfig config;

    private static final long REQUEST_DELAY_MS = 45;
    private static final long RETRY_DELAY_MS = 90;
    private static final int MAX_RETRIES = 2;

    public KotakQuoteService(
            RestTemplate restTemplate,
            KotakNeoConfig config) {

        this.restTemplate = restTemplate;
        this.config = config;
    }

    public List<KotakQuoteResponse> getOhlc(
            String exchangeSegment,
            List<String> tokens) {

        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyList();
        }

        List<KotakQuoteResponse> quotes =
                new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {

            String token = tokens.get(i);
            if(Objects.equals(token, "544937")){
                exchangeSegment="bse_cm";
            }else{
                exchangeSegment="nse_cm";
            }

            /*
             * Keep a gap between consecutive requests.
             */
            if (i > 0) {
                sleep(REQUEST_DELAY_MS);
            }

            KotakQuoteResponse quote =
                    getQuoteWithRetry(
                            exchangeSegment,
                            token
                    );

            if (quote != null) {
                quotes.add(quote);
            }
        }

        log.info(
                "Kotak quotes received: {} out of {}",
                quotes.size(),
                tokens.size()
        );

        return quotes;
    }

    private KotakQuoteResponse getQuoteWithRetry(
            String exchangeSegment,
            String token) {

        int attempt = 0;

        while (attempt <= MAX_RETRIES) {

            try {

                String url =
                        "https://e22.kotaksecurities.com"
                                + "/script-details/1.0/quotes/neosymbol/"
                                + exchangeSegment
                                + "|"
                                + token
                                + "/all";

//                log.info(
//                        "Requesting Kotak quote for token: {} "
//                                + "(attempt {}/{})",
//                        token,
//                        attempt + 1,
//                        MAX_RETRIES + 1
//                );

                HttpHeaders headers =
                        new HttpHeaders();

                headers.set(
                        "Authorization",
                        config.getConsumerKey()
                );

                HttpEntity<Void> entity =
                        new HttpEntity<>(headers);

                ResponseEntity<KotakQuoteResponse[]> response =
                        restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                entity,
                                KotakQuoteResponse[].class
                        );

                if (response.getBody() != null
                        && response.getBody().length > 0) {

                    return response.getBody()[0];
                }

                log.warn(
                        "Empty Kotak response for token: {}",
                        token
                );

                return null;

            } catch (
                    HttpClientErrorException.TooManyRequests e) {

                attempt++;

                if (attempt > MAX_RETRIES) {

                    log.error(
                            "Kotak rate limit exceeded. "
                                    + "Giving up on token: {}",
                            token
                    );

                    return null;
                }

                log.warn(
                        "Kotak rate limit reached for token: {}. "
                                + "Waiting {} ms before retry.",
                        token,
                        RETRY_DELAY_MS
                );

                sleep(RETRY_DELAY_MS);

            } catch (Exception e) {

                log.error(
                        "Failed to get Kotak quote for token: {}",
                        token,
                        e
                );

                return null;
            }
        }

        return null;
    }

    private void sleep(long milliseconds) {

        try {

            Thread.sleep(milliseconds);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            log.warn(
                    "Kotak quote request thread interrupted."
            );
        }
    }
    public KotakQuoteResponse getQuote(String token) {
        String url =
                "https://e22.kotaksecurities.com"
                        + "/script-details/1.0/quotes/neosymbol/"
                        + "bse_cm"
                        + "|"
                        + token
                        + "/all";

        HttpHeaders headers = new HttpHeaders();

        headers.set(
                "Authorization",
                config.getConsumerKey()
        );

        HttpEntity<Void> entity =
                new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

        if (response.getBody() == null) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();

        try {

            KotakQuoteResponse[] quotes =
                    objectMapper.readValue(
                            response.getBody(),
                            KotakQuoteResponse[].class
                    );

            if (quotes.length == 0) {
                return null;
            }

            return quotes[0];

        } catch (Exception e) {

            log.error(
                    "Failed to parse Kotak quote for token: {}",
                    token,
                    e
            );

            return null;
        }
    }
}