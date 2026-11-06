package com.dobby.price_alert.runners;

import com.dobby.price_alert.client.NSEClient;
import com.dobby.price_alert.dto.nse.derivatives.FiiIndexOiDto;
import com.dobby.price_alert.service.FiiIndexOiHistoryService;
import com.dobby.price_alert.service.nse.FiiIndexOiParser;
import com.dobby.price_alert.util.CheckTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
@Slf4j
@RequiredArgsConstructor
public class OpenInterestPositionRunner implements CommandLineRunner {
    private final NSEClient nseClient;
    private final FiiIndexOiParser fiiIndexOiParser;
    private final FiiIndexOiHistoryService fiiIndexOiHistoryService;

    @Override
    public void run(String... args) throws Exception {

        if (!CheckTime.isAfterMarketHours()) {
            log.info("Skipping FII Data Runner. Before 6 PM IST.");
            return;
        }

        LocalDate date = LocalDate.now(
                ZoneId.of("Asia/Kolkata")
        );

        // 1. Skip Saturday and Sunday
        DayOfWeek day = date.getDayOfWeek();

        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            log.info("Weekend detected. Skipping FII OI update for {}", date);
            return;
        }

        log.info("Starting daily FII OI update for {}", date);

        FiiIndexOiDto result;

        try {
            // 2. Fetch NSE CSV
            String csv = nseClient.getParticipantWiseOiReport(date);

            if (csv == null || csv.isBlank()) {
                log.warn("NSE FII OI data unavailable for {}. Skipping.", date);
                return;
            }

            // 3. Parse FII data
            result = fiiIndexOiParser.parse(csv, date);

        } catch (Exception e) {
            // 4. Missing report / API failure / parsing failure
            log.warn(
                    "Could not fetch FII OI data for {}. Skipping today's update: {}",
                    date,
                    e.getMessage()
            );
            return;
        }

        // 5. Update R2 history
        fiiIndexOiHistoryService.saveOrUpdate(result);

        log.info("Successfully updated FII OI history for {}", date);
    }
}
