package com.dobby.price_alert.service;

import com.dobby.price_alert.client.KotakSession;
import com.dobby.price_alert.config.KotakNeoConfig;
import com.dobby.price_alert.dto.kotak.KotakMpinValidateRequest;
import com.dobby.price_alert.dto.kotak.KotakMpinValidateResponse;
import com.dobby.price_alert.dto.kotak.KotakTotpLoginRequest;
import com.dobby.price_alert.dto.kotak.KotakTotpLoginResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;



@Service
public class KotakAuthService {

    private static final String LOGIN_URL =
            "https://mis.kotaksecurities.com/login/1.0/tradeApiLogin";

    private final RestTemplate restTemplate;
    private final KotakNeoConfig config;

    public KotakAuthService(
            RestTemplate restTemplate,
            KotakNeoConfig config) {

        this.restTemplate = restTemplate;
        this.config = config;
    }

    public KotakTotpLoginResponse loginWithTotp(String totp) {

        KotakTotpLoginRequest request =
                new KotakTotpLoginRequest(
                        config.getMobileNumber(),
                        config.getCustomerId(),
                        totp
                );

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", config.getConsumerKey());
        headers.set("neo-fin-key", "neotradeapi");

        HttpEntity<KotakTotpLoginRequest> entity =
                new HttpEntity<>(request, headers);

        ResponseEntity<KotakTotpLoginResponse> response =
                restTemplate.postForEntity(
                        LOGIN_URL,
                        entity,
                        KotakTotpLoginResponse.class
                );

        return response.getBody();
    }
    public KotakSession validateMpin(
            KotakTotpLoginResponse loginResponse) {

        KotakMpinValidateRequest request =
                new KotakMpinValidateRequest(config.getMpin());

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", config.getConsumerKey());
        headers.set("neo-fin-key", "neotradeapi");

        headers.set("sid", loginResponse.getData().getSid());
        headers.set("Auth", loginResponse.getData().getToken());

        HttpEntity<KotakMpinValidateRequest> entity =
                new HttpEntity<>(request, headers);

        ResponseEntity<KotakMpinValidateResponse> response =
                restTemplate.postForEntity(
                        "https://mis.kotaksecurities.com/login/1.0/tradeApiValidate",
                        entity,
                        KotakMpinValidateResponse.class
                );
        KotakMpinValidateResponse responseBody = response.getBody();

        if (responseBody == null || responseBody.getData() == null) {
            throw new IllegalStateException(
                    "Kotak MPIN validation returned an empty response"
            );
        }

        KotakMpinValidateResponse.Data data =
                responseBody.getData();

        return new KotakSession(
                data.getToken(),
                data.getSid(),
                data.getBaseUrl()
        );
    }
}
