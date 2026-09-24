package com.dobby.price_alert.dto.kotak;

public class KotakMpinValidateRequest {
    private String mpin;

    public KotakMpinValidateRequest() {
    }

    public KotakMpinValidateRequest(String mpin) {
        this.mpin = mpin;
    }

    public String getMpin() {
        return mpin;
    }

    public void setMpin(String mpin) {
        this.mpin = mpin;
    }
}
