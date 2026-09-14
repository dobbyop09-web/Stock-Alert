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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PortfolioSnapshotService {

    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");

    private static final String ETF_SHEET =
            "Etf";

    private final R2UploadService r2UploadService;
    private final ObjectMapper objectMapper;

    public PortfolioSnapshotService(
            R2UploadService r2UploadService,
            ObjectMapper objectMapper
    ) {
        this.r2UploadService = r2UploadService;
        this.objectMapper = objectMapper;
    }


    /*
     * ============================================================
     * LOAD CURRENT PORTFOLIO
     * ============================================================
     */

    public PortfolioData loadPortfolio(
            String portfolioObjectKey
    ) {

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


    /*
     * ============================================================
     * CALCULATE DAILY SNAPSHOT
     *
     * type = "STOCK" or "ETF"
     * ============================================================
     */

    public PortfolioSnapshot calculateSnapshot(
            PortfolioData portfolioData,
            List<DashboardStock> dashboardStocks,
            String type
    ) {

        Map<String, DashboardStock> dashboardStockMap =
                dashboardStocks.stream()
                        .collect(Collectors.toMap(
                                DashboardStock::getSymbol,
                                Function.identity(),
                                (existing, replacement) -> existing
                        ));


        BigDecimal totalInvestedValue =
                BigDecimal.ZERO;

        BigDecimal totalCurrentValue =
                BigDecimal.ZERO;

        int holdingCount = 0;


        for (PortfolioHolding holding :
                portfolioData.getHoldings()) {


            /*
             * Ignore invalid holdings.
             */
            if (holding.getQuantity() == null ||
                    holding.getBuyPrice() == null ||
                    holding.getSymbol() == null) {

                continue;
            }


            DashboardStock dashboardStock =
                    dashboardStockMap.get(
                            holding.getSymbol()
                    );


            /*
             * We need the dashboard record to determine
             * whether this is a STOCK or ETF.
             */
            if (dashboardStock == null) {
                continue;
            }


            /*
             * ETF identification is based on:
             *
             * "sheet": "Etf"
             */
            boolean isEtf =
                    ETF_SHEET.equalsIgnoreCase(
                            dashboardStock.getSheet()
                    );


            /*
             * Filter according to requested snapshot type.
             */
            if ("ETF".equalsIgnoreCase(type) && !isEtf) {
                continue;
            }

            if ("STOCK".equalsIgnoreCase(type) && isEtf) {
                continue;
            }


            /*
             * ====================================================
             * INVESTED VALUE
             * ====================================================
             */

            BigDecimal quantity =
                    BigDecimal.valueOf(
                            holding.getQuantity()
                    );


            BigDecimal investedValue =
                    holding.getBuyPrice()
                            .multiply(quantity);


            totalInvestedValue =
                    totalInvestedValue.add(
                            investedValue
                    );


            /*
             * ====================================================
             * CURRENT PRICE
             *
             * First preference:
             * latest DashboardStock price.
             *
             * Fallback:
             * currentPrice from portfolio.json.
             *
             * Final fallback:
             * buyPrice.
             *
             * This prevents a missing NSE price from creating
             * an artificial portfolio crash.
             * ====================================================
             */

            BigDecimal currentPrice;


            if (dashboardStock.getCurrentPrice() != null) {

                currentPrice =
                        dashboardStock.getCurrentPrice();

            } else if (holding.getCurrentPrice() != null) {

                currentPrice =
                        holding.getCurrentPrice();

            } else {

                currentPrice =
                        holding.getBuyPrice();
            }


            /*
             * ====================================================
             * CURRENT VALUE
             * ====================================================
             */

            BigDecimal currentValue =
                    currentPrice.multiply(quantity);


            totalCurrentValue =
                    totalCurrentValue.add(
                            currentValue
                    );


            holdingCount++;
        }


        /*
         * ========================================================
         * PROFIT / LOSS
         * ========================================================
         */

        BigDecimal profitLoss =
                totalCurrentValue.subtract(
                        totalInvestedValue
                );


        /*
         * ========================================================
         * PROFIT / LOSS %
         * ========================================================
         */

        BigDecimal profitLossPercent =
                BigDecimal.ZERO;


        if (totalInvestedValue.compareTo(
                BigDecimal.ZERO
        ) > 0) {

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


        /*
         * ========================================================
         * CREATE SNAPSHOT
         * ========================================================
         */

        return PortfolioSnapshot.builder()

                .date(
                        LocalDate.now(
                                INDIA_ZONE
                        ).toString()
                )

                .type(type)

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

                .profitLossPercent(
                        profitLossPercent
                )

                .holdingCount(
                        holdingCount
                )

                .build();
    }


    /*
     * ============================================================
     * LOAD PORTFOLIO HISTORY
     * ============================================================
     */

    public PortfolioHistory loadPortfolioHistory(
            String historyObjectKey
    ) {

        Path historyFile =
                r2UploadService.download(
                        historyObjectKey
                );


        /*
         * History file does not exist yet.
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


    /*
     * ============================================================
     * ADD OR UPDATE DAILY SNAPSHOT
     *
     * Date + Type together identify a snapshot.
     *
     * Example:
     *
     * 2026-09-14 + STOCK
     * 2026-09-14 + ETF
     *
     * ============================================================
     */

    public PortfolioHistory updateDailySnapshot(
            PortfolioHistory portfolioHistory,
            PortfolioSnapshot newSnapshot
    ) {

        boolean snapshotUpdated = false;


        for (int i = 0;
             i < portfolioHistory.getSnapshots().size();
             i++) {


            PortfolioSnapshot existingSnapshot =
                    portfolioHistory
                            .getSnapshots()
                            .get(i);


            boolean sameDate =
                    existingSnapshot.getDate()
                            .equals(
                                    newSnapshot.getDate()
                            );


            boolean sameType =
                    existingSnapshot.getType()
                            .equalsIgnoreCase(
                                    newSnapshot.getType()
                            );


            /*
             * Same date + same type:
             * replace today's snapshot.
             */
            if (sameDate && sameType) {

                portfolioHistory
                        .getSnapshots()
                        .set(
                                i,
                                newSnapshot
                        );


                snapshotUpdated = true;

                break;
            }
        }


        /*
         * No snapshot for this date/type:
         * add a new one.
         */
        if (!snapshotUpdated) {

            portfolioHistory
                    .getSnapshots()
                    .add(
                            newSnapshot
                    );
        }


        /*
         * Update history timestamp.
         */
        portfolioHistory.setLastUpdated(
                Instant.now().toString()
        );


        return portfolioHistory;
    }


    /*
     * ============================================================
     * SAVE PORTFOLIO HISTORY TO R2
     * ============================================================
     */

    public void savePortfolioHistory(
            PortfolioHistory portfolioHistory,
            String historyObjectKey
    ) {

        Path tempFile = null;


        try {

            tempFile =
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


        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to save portfolio history to R2: "
                            + historyObjectKey,
                    e
            );

        } finally {

            if (tempFile != null) {

                try {

                    Files.deleteIfExists(
                            tempFile
                    );

                } catch (Exception ignored) {
                    // Nothing else to do here.
                }
            }
        }
    }


    /*
     * ============================================================
     * SAVE DAILY SNAPSHOT
     * ============================================================
     */

    public void saveDailySnapshot(
            String historyObjectKey,
            PortfolioSnapshot snapshot
    ) {

        PortfolioHistory portfolioHistory =
                loadPortfolioHistory(
                        historyObjectKey
                );


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