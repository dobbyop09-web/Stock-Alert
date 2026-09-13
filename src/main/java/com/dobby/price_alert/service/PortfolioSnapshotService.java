package com.dobby.price_alert.service;


import com.dobby.price_alert.dto.DashboardStock;
import com.dobby.price_alert.dto.portfolio.PortfolioData;
import com.dobby.price_alert.dto.portfolio.PortfolioHistory;
import com.dobby.price_alert.dto.portfolio.PortfolioHolding;
import com.dobby.price_alert.dto.portfolio.PortfolioSnapshot;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PortfolioSnapshotService {

    private final R2UploadService r2UploadService;
    private final ObjectMapper objectMapper;

    public PortfolioSnapshotService(
            R2UploadService r2UploadService,
            ObjectMapper objectMapper
    ) {
        this.r2UploadService = r2UploadService;
        this.objectMapper = objectMapper;
    }


    public PortfolioData loadPortfolio(String portfolioObjectKey) {

        Path portfolioFile =
                r2UploadService.download(portfolioObjectKey);

        if (portfolioFile == null) {
            throw new RuntimeException(
                    "Portfolio file not found in R2: "
                            + portfolioObjectKey
            );
        }

        try {

            PortfolioData portfolioData =
                    objectMapper.readValue(
                            Files.readString(portfolioFile),
                            PortfolioData.class
                    );

            Files.deleteIfExists(portfolioFile);

            return portfolioData;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to read portfolio data from R2: "
                            + portfolioObjectKey,
                    e
            );
        }
    }
    public PortfolioSnapshot calculateSnapshot(
            PortfolioData portfolioData,
            List<DashboardStock> dashboardStocks
    ) {

        Map<String, DashboardStock> dashboardStockMap =
                dashboardStocks.stream()
                        .collect(Collectors.toMap(
                                DashboardStock::getSymbol,
                                Function.identity(),
                                (existing, replacement) -> existing
                        ));


        BigDecimal totalInvestedValue = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;

        int holdingCount = 0;


        for (PortfolioHolding holding : portfolioData.getHoldings()) {

            if (holding.getQuantity() == null ||
                    holding.getBuyPrice() == null) {
                continue;
            }


            BigDecimal quantity =
                    BigDecimal.valueOf(holding.getQuantity());


            // BUY PRICE × QUANTITY
            BigDecimal investedValue =
                    holding.getBuyPrice()
                            .multiply(quantity);


            totalInvestedValue =
                    totalInvestedValue.add(investedValue);


            DashboardStock dashboardStock =
                    dashboardStockMap.get(holding.getSymbol());


            /*
             * If the stock is not available in the
             * latest dashboard data, skip its current value.
             */
            if (dashboardStock == null ||
                    dashboardStock.getCurrentPrice() == null) {

                continue;
            }


            BigDecimal currentPrice = dashboardStock.getCurrentPrice();



            // CURRENT PRICE × QUANTITY
            BigDecimal currentValue =
                    currentPrice.multiply(quantity);


            totalCurrentValue =
                    totalCurrentValue.add(currentValue);


            holdingCount++;
        }


        BigDecimal profitLoss =
                totalCurrentValue.subtract(totalInvestedValue);


        BigDecimal profitLossPercent =
                BigDecimal.ZERO;


        if (totalInvestedValue.compareTo(BigDecimal.ZERO) > 0) {

            profitLossPercent =
                    profitLoss
                            .divide(
                                    totalInvestedValue,
                                    6,
                                    RoundingMode.HALF_UP
                            )
                            .multiply(
                                    BigDecimal.valueOf(100)
                            )
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );
        }


        return PortfolioSnapshot.builder()
                .date(
                        LocalDate.now(
                                ZoneId.of("Asia/Kolkata")
                        ).toString()
                )
                .investedValue(
                        totalInvestedValue.setScale(
                                2,
                                RoundingMode.HALF_UP
                        )
                )
                .currentValue(
                        totalCurrentValue.setScale(
                                2,
                                RoundingMode.HALF_UP
                        )
                )
                .profitLoss(
                        profitLoss.setScale(
                                2,
                                RoundingMode.HALF_UP
                        )
                )
                .profitLossPercent(profitLossPercent)
                .holdingCount(holdingCount)
                .build();
    }
    public PortfolioHistory loadPortfolioHistory(
            String historyObjectKey
    ) {

        Path historyFile =
                r2UploadService.download(historyObjectKey);

        /*
         * First time:
         * The history file may not exist in R2 yet.
         */
        if (historyFile == null) {
            return PortfolioHistory.builder()
                    .build();
        }

        try {

            PortfolioHistory portfolioHistory =
                    objectMapper.readValue(
                            Files.readString(historyFile),
                            PortfolioHistory.class
                    );

            Files.deleteIfExists(historyFile);

            return portfolioHistory;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to read portfolio history from R2: "
                            + historyObjectKey,
                    e
            );
        }
    }
    public PortfolioHistory updateDailySnapshot(
            PortfolioHistory portfolioHistory,
            PortfolioSnapshot newSnapshot
    ) {

        boolean snapshotUpdated = false;


        for (int i = 0;
             i < portfolioHistory.getSnapshots().size();
             i++) {

            PortfolioSnapshot existingSnapshot =
                    portfolioHistory.getSnapshots().get(i);


            if (existingSnapshot.getDate()
                    .equals(newSnapshot.getDate())) {

                portfolioHistory.getSnapshots().set(
                        i,
                        newSnapshot
                );

                snapshotUpdated = true;

                break;
            }
        }


        /*
         * No snapshot for today.
         * Add a new daily snapshot.
         */
        if (!snapshotUpdated) {

            portfolioHistory.getSnapshots()
                    .add(newSnapshot);
        }


        portfolioHistory.setLastUpdated(
                java.time.Instant.now().toString()
        );


        return portfolioHistory;
    }
    public void savePortfolioHistory(
            PortfolioHistory portfolioHistory,
            String historyObjectKey
    ) {

        try {

            Path tempFile =
                    Files.createTempFile(
                            "portfolio-history-",
                            ".json"
                    );


            objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(
                            tempFile.toFile(),
                            portfolioHistory
                    );


            r2UploadService.upload(
                    tempFile,
                    historyObjectKey
            );


            Files.deleteIfExists(tempFile);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to save portfolio history to R2: "
                            + historyObjectKey,
                    e
            );
        }
    }
    public void saveDailySnapshot(
            String historyObjectKey,
            PortfolioSnapshot snapshot
    ) {

        PortfolioHistory portfolioHistory =
                loadPortfolioHistory(historyObjectKey);


        PortfolioHistory updatedHistory =
                updateDailySnapshot(
                        portfolioHistory,
                        snapshot
                );


        savePortfolioHistory(
                updatedHistory,
                historyObjectKey
        );
    }
}