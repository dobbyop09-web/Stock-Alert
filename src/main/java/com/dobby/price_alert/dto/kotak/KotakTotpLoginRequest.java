package com.dobby.price_alert.dto.kotak;

public class KotakTotpLoginRequest {
    private String mobileNumber;
    private String ucc;
    private String totp;

    public KotakTotpLoginRequest() {
    }

    public KotakTotpLoginRequest(String mobileNumber, String ucc, String totp) {
        this.mobileNumber = mobileNumber;
        this.ucc = ucc;
        this.totp = totp;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getUcc() {
        return ucc;
    }

    public void setUcc(String ucc) {
        this.ucc = ucc;
    }

    public String getTotp() {
        return totp;
    }

    public void setTotp(String totp) {
        this.totp = totp;
    }
}
