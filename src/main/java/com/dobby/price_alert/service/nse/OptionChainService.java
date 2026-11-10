package com.dobby.price_alert.service.nse;

import com.dobby.price_alert.client.NSEClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class OptionChainService {

    private final NSEClient nseClient;

    private static final int EXPIRY_COUNT = 3;

    private static final ZoneId NSE_ZONE =
            ZoneId.of("Asia/Kolkata");

    private static final DateTimeFormatter EXPIRY_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    public OptionChainService(NSEClient nseClient) {
        this.nseClient = nseClient;
    }

    public Map<String, Object> fetchOptionChainSnapshot(String symbol) {

        LocalDate snapshotDate = LocalDate.now(NSE_ZONE);
        OffsetDateTime capturedAt = OffsetDateTime.now(NSE_ZONE);

        // Step 1: Fetch available expiry dates
        Map<String, Object> contractInfo =
                nseClient.getOptionChainContractInfo(symbol);

        // Step 2: Select nearest three upcoming expiries
        List<LocalDate> selectedExpiries =
                selectUpcomingExpiries(contractInfo, snapshotDate);

        if (selectedExpiries.isEmpty()) {
            throw new RuntimeException(
                    "No upcoming expiries found for " + symbol
            );
        }

        if (selectedExpiries.size() < EXPIRY_COUNT) {
            log.warn(
                    "Only {} upcoming expiries found for {}",
                    selectedExpiries.size(), symbol
            );
        }

        List<Map<String, Object>> expiryResults = new ArrayList<>();

        // Step 3: Fetch individual option chains
        for (LocalDate expiry : selectedExpiries) {

            String expiryDate = expiry.format(EXPIRY_FORMATTER);

            try {

                Map<String, Object> response =
                        nseClient.getIndexOptionChain(symbol, expiryDate);

                validateResponse(response);

                Map<String, Object> expiryResult = new LinkedHashMap<>();

                expiryResult.put("expiryDate", expiryDate);
                expiryResult.put("status", "SUCCESS");

                // Preserve the complete NSE response
                expiryResult.put("response", response);

                expiryResults.add(expiryResult);

                log.info(
                        "Successfully fetched option chain for {} expiry {}",
                        symbol, expiryDate
                );

            } catch (Exception e) {

                log.error(
                        "Failed to fetch option chain for {} expiry {}: {}",
                        symbol, expiryDate, e.getMessage()
                );

                Map<String, Object> failedResult = new LinkedHashMap<>();

                failedResult.put("expiryDate", expiryDate);
                failedResult.put("status", "FAILED");
                failedResult.put("error", e.getMessage());

                expiryResults.add(failedResult);
            }
        }

        // Step 4: Combine all expiries into one response
        Map<String, Object> snapshot = new LinkedHashMap<>();

        snapshot.put("symbol", symbol);
        snapshot.put("snapshotDate", snapshotDate.toString());
        snapshot.put("capturedAt", capturedAt.toString());
        snapshot.put("expiryCount", expiryResults.size());
        snapshot.put("expiries", expiryResults);

        return snapshot;
    }

    private List<LocalDate> selectUpcomingExpiries(
            Map<String, Object> contractInfo,
            LocalDate today) {

        Object expiryObject = contractInfo.get("expiryDates");

        if (!(expiryObject instanceof List<?> expiryDates)) {
            throw new RuntimeException(
                    "Expiry dates not found in NSE response"
            );
        }

        List<LocalDate> upcomingExpiries = new ArrayList<>();

        for (Object value : expiryDates) {

            if (value == null) {
                continue;
            }

            try {

                LocalDate expiry = LocalDate.parse(
                        value.toString(),
                        EXPIRY_FORMATTER
                );

                if (!expiry.isBefore(today)) {
                    upcomingExpiries.add(expiry);
                }

            } catch (DateTimeParseException e) {

                log.warn("Ignoring invalid NSE expiry date: {}", value);
            }
        }

        return upcomingExpiries.stream()
                .distinct()
                .sorted()
                .limit(EXPIRY_COUNT)
                .toList();
    }

    private void validateResponse(Map<String, Object> response) {

        if (response == null) {
            throw new RuntimeException("Empty option chain response");
        }

        Object recordsObject = response.get("records");

        if (!(recordsObject instanceof Map<?, ?> records)) {
            throw new RuntimeException(
                    "Missing records in NSE option chain response"
            );
        }

        if (!(records.get("data") instanceof List<?>)) {
            throw new RuntimeException(
                    "Missing option chain data in NSE response"
            );
        }
    }
}
