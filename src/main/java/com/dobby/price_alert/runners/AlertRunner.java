package com.dobby.price_alert.runners;

import com.dobby.price_alert.constants.PortfolioConstants;
import com.dobby.price_alert.constants.SheetType;
import com.dobby.price_alert.dto.DashboardStock;
import com.dobby.price_alert.dto.portfolio.PortfolioData;
import com.dobby.price_alert.dto.portfolio.PortfolioSnapshot;
import com.dobby.price_alert.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class AlertRunner implements CommandLineRunner {
    private final CsvReaderService csvReaderService;
    private static final Logger log =
            LoggerFactory.getLogger(CsvReaderService.class);
    private final DashboardJsonService dashboardJsonService;

    private final DashBoardMetaDataService dashBoardMetaDataService;
    private final StockAlertService stockAlertService;
    private final PortfolioSnapshotService portfolioSnapshotService;

    List<DashboardStock> dashboard = new ArrayList<>();

    public AlertRunner(CsvReaderService csvReaderService, DashboardJsonService dashboardJsonService, DashBoardMetaDataService dashBoardMetaDataService, StockAlertService stockAlertService, PortfolioSnapshotService portfolioSnapshotService
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
        Set<String> triggeredToday = stockAlertService.getAllTriggeredToday();

        for(SheetType sheet: SheetType.values()){
            dashboard.addAll(
                    csvReaderService.readCsvAndCheckAlerts(
                            sheet.getSheetConfig(),
                            triggeredToday
                    )
            );
        }
        log.info("Preparing to write dashboard JSON...");
        log.info("Total dashboard records: {}", dashboard.size());

        dashboardJsonService.write(dashboard);
        updatePortfolioSnapshots();

        long endTime = System.currentTimeMillis();
        dashBoardMetaDataService.buildAndWrite(endTime - startTime);
        log.info("========== STOCK ALERT JOB COMPLETED ==========");
    }
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
    private void updatePortfolioSnapshot(
            String portfolioObjectKey,
            String historyObjectKey
    ) {

        try {

            PortfolioData portfolioData =
                    portfolioSnapshotService.loadPortfolio(
                            portfolioObjectKey
                    );


            PortfolioSnapshot snapshot =
                    portfolioSnapshotService.calculateSnapshot(
                            portfolioData,
                            dashboard
                    );


            portfolioSnapshotService.saveDailySnapshot(
                    historyObjectKey,
                    snapshot
            );


            log.info(
                    "Portfolio snapshot updated successfully: {}",
                    portfolioObjectKey
            );


            log.info(
                    "Invested Value: {}, Current Value: {}, P&L: {}%",
                    snapshot.getInvestedValue(),
                    snapshot.getCurrentValue(),
                    snapshot.getProfitLossPercent()
            );

        } catch (Exception e) {

            /*
             * Don't fail the entire stock dashboard job
             * if one portfolio snapshot fails.
             */
            log.error(
                    "Failed to update portfolio snapshot: {}",
                    portfolioObjectKey,
                    e
            );
        }
    }
}
