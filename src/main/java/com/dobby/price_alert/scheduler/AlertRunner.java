package com.dobby.price_alert.scheduler;

import com.dobby.price_alert.constants.SheetType;
import com.dobby.price_alert.dto.DashboardStock;
import com.dobby.price_alert.service.CsvReaderService;
import com.dobby.price_alert.service.DashboardJsonService;
import com.dobby.price_alert.service.StockAlertService;
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

    private final StockAlertService stockAlertService;
    List<DashboardStock> dashboard = new ArrayList<>();
    public AlertRunner(CsvReaderService csvReaderService, DashboardJsonService dashboardJsonService, StockAlertService stockAlertService
    ) {
        this.csvReaderService = csvReaderService;
        this.dashboardJsonService = dashboardJsonService;
        this.stockAlertService = stockAlertService;
    }
    @Override
    public void run(String... args) throws Exception {
        log.info("========== STOCK ALERT JOB STARTED ==========");
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
        log.info("========== STOCK ALERT JOB COMPLETED ==========");
    }
}
