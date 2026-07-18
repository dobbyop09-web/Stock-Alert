package com.dobby.price_alert.constants;

import com.dobby.price_alert.dto.SheetConfig;

public enum SheetType {
    DEFENCE("Defence", "1003001524"),

    BANK("Bank", "1209852281"),

    AUTO("Auto", "522202110"),

    MANUFACTURING("Manufacturing", "1548732787"),

    IT("It", "1604399595"),

    CAPITALMARKET("CapitalMarket", "1472211126"),
    FMCG("Fmcg", "1254989583"),

    HEALTH("Health", "1954455770"),

    METAL("Metal", "679636049"),

    ENERGY("Energy", "1475720491"),

    SMALLCAP("SmallCap", "2029475330"),

    MISCELLANEOUS("Miscellaneous", "709000981");



    private final SheetConfig sheetConfig;

    SheetType(String name, String gid) {
        this.sheetConfig = new SheetConfig(name, gid);
    }

    public SheetConfig getSheetConfig() {
        return sheetConfig;
    }

    public String getName() {
        return sheetConfig.getName();
    }

    public String getGid() {
        return sheetConfig.getGid();
    }

    public String getUrl() {
        return sheetConfig.getUrl();
    }
}
