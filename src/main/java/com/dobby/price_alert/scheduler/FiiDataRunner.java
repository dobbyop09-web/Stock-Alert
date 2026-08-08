package com.dobby.price_alert.scheduler;

import com.dobby.price_alert.service.CsvReaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class FiiDataRunner implements CommandLineRunner {
    private static final Logger log =
            LoggerFactory.getLogger(CsvReaderService.class);

    @Autowired
    private com.dobby.price_alert.service.FiiDataService fiiDataService;
    @Override
    public void run(String... args) throws Exception {
        log.info("Starting FII/DII update...");

        fiiDataService.updateFiiData();

        log.info("FII/DII update completed.");
    }
}
