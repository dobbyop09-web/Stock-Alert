package com.dobby.price_alert.client;

public class KotakSession {
    private final String token;
    private final String sid;
    private final String baseUrl;

    public KotakSession(String token, String sid, String baseUrl) {
        this.token = token;
        this.sid = sid;
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }

    public String getSid() {
        return sid;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
