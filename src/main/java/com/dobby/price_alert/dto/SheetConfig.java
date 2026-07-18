package com.dobby.price_alert.dto;

public class SheetConfig {
    private final String name;
    private final String gid;
    private static final String SHEET_BASE_URL =
            "https://docs.google.com/spreadsheets/d/1AQ93bxs1qthy6WSqtkV-DhMYjAMaZrsQLLabk_KGgyQ/export?format=csv&gid=";

    public String getGid() {
        return gid;
    }

    public SheetConfig(String name, String gid) {
        this.name = name;
        this.gid = gid;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return SHEET_BASE_URL+gid;
    }
}
