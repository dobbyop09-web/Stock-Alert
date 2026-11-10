package com.dobby.price_alert.runners;

import com.dobby.price_alert.service.CsvReaderService;
import com.dobby.price_alert.util.CheckTime;
import org.hibernate.annotations.Check;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
@Order(3)
@Component
@ConditionalOnProperty(
        name = "app.runners.fii-data.enabled",
        havingValue = "true"
)
public class FiiDataRunner implements CommandLineRunner {
    private static final Logger log =
            LoggerFactory.getLogger(CsvReaderService.class);

    @Autowired
    private com.dobby.price_alert.service.FiiDataService fiiDataService;
    @Override
    public void run(String... args) throws Exception {
        log.info("Starting FII/DII update...");
        if (!CheckTime.isAfterMarketHours()) {
            log.info("Skipping FII Data Runner. Before 6 PM IST.");
            return;
        }
        fiiDataService.updateFiiData();

        log.info("FII/DII update completed.");
    }
}
