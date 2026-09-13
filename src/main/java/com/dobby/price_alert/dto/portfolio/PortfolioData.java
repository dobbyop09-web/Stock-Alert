package com.dobby.price_alert.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PortfolioData {

    @Builder.Default
    private List<PortfolioHolding> holdings = new ArrayList<>();

    @Builder.Default
    private List<ClosedTransaction> closedTransactions = new ArrayList<>();

    private String lastUpdated;
}
