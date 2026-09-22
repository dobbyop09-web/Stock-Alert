package com.dobby.price_alert.dto.nse.derivatives;

import lombok.Data;

import java.util.List;

@Data
public class DerivativesResponse {

    private List<DerivativesDataDto> data;

    private String timestamp;
}