package com.dobby.price_alert.dto.nse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NseResponse {
   private List<EquityResponse> equityResponse;

}
