package com.dobby.price_alert.runners;

import com.dobby.price_alert.constants.PortfolioConstants;
import com.dobby.price_alert.constants.SheetType;
import com.dobby.price_alert.dto.DashboardStock;
import com.dobby.price_alert.dto.portfolio.PortfolioData;
import com.dobby.price_alert.dto.portfolio.PortfolioSnapshot;
import com.dobby.price_alert.service.CsvReaderService;
import com.dobby.price_alert.service.DashBoardMetaDataService;
import com.dobby.price_alert.service.DashboardJsonService;
import com.dobby.price_alert.service.PortfolioSnapshotService;
import com.dobby.price_alert.service.StockAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@Order(1)
public class AlertRunner implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(AlertRunner.class);

    private final CsvReaderService csvReaderService;
    private final DashboardJsonService dashboardJsonService;
    private final DashBoardMetaDataService dashBoardMetaDataService;
    private final StockAlertService stockAlertService;
    private final PortfolioSnapshotService portfolioSnapshotService;

    private final List<DashboardStock> dashboard = new ArrayList<>();

    public AlertRunner(
            CsvReaderService csvReaderService,
            DashboardJsonService dashboardJsonService,
            DashBoardMetaDataService dashBoardMetaDataService,
            StockAlertService stockAlertService,
            PortfolioSnapshotService portfolioSnapshotService
    ) {
        this.csvReaderService = csvReaderService;
        this.dashboardJsonService = dashboardJsonService;
        this.dashBoardMetaDataService = dashBoardMetaDataService;
        this.stockAlertService = stockAlertService;
        this.portfolioSnapshotService = portfolioSnapshotService;
    }

    @Override
    public void run(String... args) throws Exception {

        log.info("========== STOCK ALERT JOB STARTED ==========");

        long startTime = System.currentTimeMillis();

        Set<String> triggeredToday =
                stockAlertService.getAllTriggeredToday();

        /*
         * Read all configured sheets.
         */
        for (SheetType sheet : SheetType.values()) {

            dashboard.addAll(
                    csvReaderService.readCsvAndCheckAlerts(
                            sheet.getSheetConfig(),
                            triggeredToday
                    )
            );
        }

        log.info("Preparing to write dashboard JSON...");
        log.info("Total dashboard records: {}", dashboard.size());

        /*
         * Write dashboard JSON.
         */
        dashboardJsonService.write(dashboard);

        /*
         * Update portfolio daily snapshots.
         */
        DayOfWeek today = LocalDate.now().getDayOfWeek();

        if (today != DayOfWeek.SATURDAY && today != DayOfWeek.SUNDAY) {
            updatePortfolioSnapshots();
        }

        long endTime = System.currentTimeMillis();

        dashBoardMetaDataService.buildAndWrite(
                endTime - startTime
        );

        log.info(
                "========== STOCK ALERT JOB COMPLETED =========="
        );
    }


    /*
     * ============================================================
     * UPDATE ALL PORTFOLIO SNAPSHOTS
     * ============================================================
     */

    private void updatePortfolioSnapshots() {

        updatePortfolioSnapshot(
                PortfolioConstants.DOBBY_PORTFOLIO,
                PortfolioConstants.DOBBY_PORTFOLIO_HISTORY
        );

        updatePortfolioSnapshot(
                PortfolioConstants.AMAN_PORTFOLIO,
                PortfolioConstants.AMAN_PORTFOLIO_HISTORY
        );
    }


    /*
     * ============================================================
     * UPDATE ONE PORTFOLIO
     * ============================================================
     */

    private void updatePortfolioSnapshot(
            String portfolioObjectKey,
            String historyObjectKey
    ) {

        try {

            /*
             * Load current portfolio.
             */
            PortfolioData portfolioData =
                    portfolioSnapshotService.loadPortfolio(
                            portfolioObjectKey
                    );


            /*
             * ====================================================
             * STOCK SNAPSHOT
             * ====================================================
             */

            PortfolioSnapshot stockSnapshot =
                    portfolioSnapshotService.calculateSnapshot(
                            portfolioData,
                            dashboard,
                            "STOCK"
                    );


            portfolioSnapshotService.saveDailySnapshot(
                    historyObjectKey,
                    stockSnapshot
            );


            log.info(
                    "STOCK portfolio snapshot updated: {}",
                    portfolioObjectKey
            );

            log.info(
                    "STOCK | Invested: {}, Current: {}, P&L: {}%",
                    stockSnapshot.getInvestedValue(),
                    stockSnapshot.getCurrentValue(),
                    stockSnapshot.getProfitLossPercent()
            );


            /*
             * ====================================================
             * ETF SNAPSHOT
             * ====================================================
             */

            PortfolioSnapshot etfSnapshot =
                    portfolioSnapshotService.calculateSnapshot(
                            portfolioData,
                            dashboard,
                            "ETF"
                    );


            portfolioSnapshotService.saveDailySnapshot(
                    historyObjectKey,
                    etfSnapshot
            );


            log.info(
                    "ETF portfolio snapshot updated: {}",
                    portfolioObjectKey
            );

            log.info(
                    "ETF | Invested: {}, Current: {}, P&L: {}%",
                    etfSnapshot.getInvestedValue(),
                    etfSnapshot.getCurrentValue(),
                    etfSnapshot.getProfitLossPercent()
            );


        } catch (Exception e) {

            /*
             * Don't fail the entire stock dashboard job
             * if portfolio snapshot processing fails.
             */
            log.error(
                    "Failed to update portfolio snapshot: {}",
                    portfolioObjectKey,
                    e
            );
        }
    }
}