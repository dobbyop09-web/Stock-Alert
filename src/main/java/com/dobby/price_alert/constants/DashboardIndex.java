package com.dobby.price_alert.constants;

import lombok.Getter;

@Getter
public enum DashboardIndex {
    NIFTY50("NIFTY 50", "NIFTY"),
    BANKNIFTY("NIFTY BANK", "BANKNIFTY"),
    SMALLCAP250("NIFTY SMALLCAP 250", "SMALLCA250"),
    AUTO("NIFTY AUTO", "CNXAUTO"),
    FMCG("NIFTY FMCG", "CNXFMCG"),
    PSUBANK("NIFTY PSU BANK", "CNXPSUBANK"),
    ENERGY("NIFTY ENERGY", "CNXENERGY"),
    METAL("NIFTY METAL", "CNXMETAL"),
    OIL_GAS("NIFTY OIL & GAS", "NIFTOILGAS"),
    HEALTHCARE("NIFTY HEALTHCARE INDEX", "NFTHEALTHC"),
    DEFENCE("NIFTY INDIA DEFENCE", "NIFINDDEFE"),
    IT("NIFTY IT", "CNXIT"),
    CAPITAL_MARKETS("NIFTY CAPITAL MARKETS", "NIFCAPMARK");

    private final String nseName;
    private final String screenerSlug;

    DashboardIndex(String nseName, String screenerSlug) {
        this.nseName = nseName;
        this.screenerSlug = screenerSlug;
    }
}
