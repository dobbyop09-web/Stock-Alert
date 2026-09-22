package com.dobby.price_alert.dto.nse.derivatives;

import lombok.Data;

import java.util.List;

@Data
public class DerivativesStockList {

    private List<String> stocks;
}