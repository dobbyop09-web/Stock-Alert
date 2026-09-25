package com.dobby.price_alert.service.kotak;

import com.dobby.price_alert.client.KotakSession;
import com.dobby.price_alert.config.KotakNeoConfig;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class KotakScripMasterApiService {
    private final RestTemplate restTemplate;
    private final KotakNeoConfig config;

    public KotakScripMasterApiService(RestTemplate restTemplate, KotakNeoConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }

    public String getMasterScripFilePaths(KotakSession session) {

        String url =
                session.getBaseUrl()
                        + "/script-details/1.0/masterscrip/file-paths";

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
                        org.springframework.http.HttpMethod.GET,
                        entity,
                        String.class
                );

        return response.getBody();
    }

    public String downloadNseCashMaster(String fileUrl) {

        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        fileUrl,
                        String.class
                );

        return response.getBody();
    }
}
