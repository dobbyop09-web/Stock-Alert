package com.dobby.price_alert.scheduler;

import com.dobby.price_alert.constants.SheetType;
import com.dobby.price_alert.dto.SheetConfig;
import com.dobby.price_alert.service.CsvReaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public class AlertRunner implements CommandLineRunner {
    private final CsvReaderService csvReaderService;
    private static final Logger log =
            LoggerFactory.getLogger(CsvReaderService.class);
    public AlertRunner(CsvReaderService csvReaderService,
                       List<SheetConfig> sheetConfigs) {
        this.csvReaderService = csvReaderService;
    }
    @Override
    public void run(String... args) throws Exception {
        log.info("========== STOCK ALERT JOB STARTED ==========");
        for(SheetType sheet: SheetType.values()){
            csvReaderService.readCsvAndCheckAlerts(sheet.getSheetConfig());
        }
        log.info("========== STOCK ALERT JOB COMPLETED ==========");
    }
}
