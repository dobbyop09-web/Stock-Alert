package com.dobby.price_alert.runner;

import com.dobby.price_alert.client.NSEClient;
import com.dobby.price_alert.dto.nse.derivatives.FiiIndexOiDto;
import com.dobby.price_alert.service.nse.FiiIndexOiParser;
import com.dobby.price_alert.service.FiiIndexOiHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class NseOiTestRunner implements ApplicationRunner {

    private final NSEClient nseClient;
    private final FiiIndexOiParser fiiIndexOiParser;
    private final FiiIndexOiHistoryService fiiIndexOiHistoryService;

    @Override
    public void run(ApplicationArguments args) throws Exception {

//        ZoneId zoneId = ZoneId.of("Asia/Kolkata");
//
//        LocalDate endDate = LocalDate.now(zoneId);
//        LocalDate startDate = endDate.minusMonths(6);
//
//        int successful = 0;
//        int failed = 0;
//
//        log.info("Starting FII OI history backfill from {} to {}",
//
//
//
//                startDate, endDate);
//
//        for (LocalDate date = startDate;
//             !date.isAfter(endDate);
//             date = date.plusDays(1)) {
//
//            // Skip Saturday and Sunday
//            DayOfWeek day = date.getDayOfWeek();
//
//            if (day == DayOfWeek.SATURDAY ||
//                    day == DayOfWeek.SUNDAY) {
//
//                continue;
//            }
//
//            try {
//                log.info("Fetching FII OI data for {}", date);
//
//                // 1. Fetch NSE CSV
//                String csv = nseClient.getParticipantWiseOiReport(date);
//
//                // 2. Parse FII data
//                FiiIndexOiDto result =
//                        fiiIndexOiParser.parse(csv, date);
//
//                // 3. Save/update R2 history
//                fiiIndexOiHistoryService.saveOrUpdate(result);
//
//                successful++;
//
//                log.info("Successfully saved FII OI data for {}", date);
//
//            } catch (Exception e) {
//
//                failed++;
//
//                log.warn("Could not fetch/process FII OI data for {}: {}",
//                        date, e.getMessage());
//            }
//
//            // Small delay to avoid sending requests too quickly
//            Thread.sleep(1200);
//        }
//
//        log.info(
//                "FII OI backfill completed. Successful: {}, Failed: {}",
//                successful, failed
//        );
   }
}