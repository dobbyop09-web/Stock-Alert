package com.dobby.price_alert.dto.kotak;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KotakTotpLoginResponse {

    private Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {

        private String token;
        private String sid;

        @JsonProperty("kType")
        private String kType;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getSid() {
            return sid;
        }

        public void setSid(String sid) {
            this.sid = sid;
        }

        public String getKType() {
            return kType;
        }

        public void setKType(String kType) {
            this.kType = kType;
        }
    }
}