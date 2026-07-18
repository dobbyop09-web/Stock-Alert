package com.dobby.price_alert.scheduler;

import com.dobby.price_alert.constants.SheetType;
import com.dobby.price_alert.service.CsvReaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PriceScheduler {
    @Autowired
    private CsvReaderService csvReaderService;
    private static final Logger log =
            LoggerFactory.getLogger(CsvReaderService.class);

    @Scheduled(fixedDelay = 10000)
    public void checkPrices() throws IOException {
        log.info("Scheduler started");
        for(SheetType sheet: SheetType.values()){
            csvReaderService.readCsvAndCheckAlerts(sheet.getSheetConfig());
        }
        log.info("Scheduler finished");


    }
}
