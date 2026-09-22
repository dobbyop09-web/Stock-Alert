package com.dobby.price_alert.dto.bse;

import lombok.Data;

@Data
public class BseQuoteResponse {

    private CurrRate CurrRate;
    private Cmpname Cmpname;
    private Header Header;
}