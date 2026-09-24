package com.dobby.price_alert.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Component
public class KotakNeoConfig {

    @Value("${kotak.neo.consumer-key}")
    private String consumerKey;

    @Value("${kotak.neo.customer-id}")
    private String customerId;

    @Value("${kotak.neo.mpin}")
    private String mpin;

    @Value("${kotak.neo.mobile-number}")
    private String mobileNumber;

    @Value("${kotak.neo.totp-secret}")
    private String totpSecret;

    public String getTotpSecret() {
        return totpSecret;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getMpin() {
        return mpin;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }
}
