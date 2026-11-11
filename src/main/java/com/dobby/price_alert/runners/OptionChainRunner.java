package com.dobby.price_alert.runners;


import com.dobby.price_alert.service.nse.OptionChainService;
import com.dobby.price_alert.service.nse.OptionChainStorageService;
import com.dobby.price_alert.util.CheckTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.runners.option-chain.enabled",
        havingValue = "true"
)
public class OptionChainRunner implements CommandLineRunner {

    private final OptionChainService optionChainService;
    private final OptionChainStorageService optionChainStorageService;

    private static final String SYMBOL = "NIFTY";

    private static final ZoneId ZONE =
            ZoneId.of("Asia/Kolkata");

    public OptionChainRunner(
            OptionChainService optionChainService,
            OptionChainStorageService optionChainStorageService) {

        this.optionChainService = optionChainService;
        this.optionChainStorageService = optionChainStorageService;
    }

    @Override
    public void run(String... args) {

        LocalDate today = LocalDate.now(ZONE);

        // Skip weekends
        DayOfWeek day = today.getDayOfWeek();

        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            log.info("Skipping NIFTY option chain runner. Weekend: {}", today);
            return;
        }

        log.info("========== NIFTY OPTION CHAIN RUNNER STARTED ==========");

        try {

            // Fetch nearest three expiry chains
            Map<String, Object> snapshot =
                    optionChainService.fetchOptionChainSnapshot(SYMBOL);

            // Upload combined JSON to R2
            String objectKey =
                    optionChainStorageService.saveSnapshot(snapshot);

            log.info(
                    "NIFTY option chain successfully updated in R2: {}",
                    objectKey
            );

        } catch (Exception e) {

            log.error(
                    "NIFTY option chain runner failed for {}: {}",
                    today,
                    e.getMessage(),
                    e
            );

        }

        log.info("========== NIFTY OPTION CHAIN RUNNER COMPLETED ==========");
    }
}

