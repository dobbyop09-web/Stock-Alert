package com.dobby.price_alert.service.nse;


import com.dobby.price_alert.service.R2UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OptionChainStorageService {

    private final R2UploadService r2UploadService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OptionChainStorageService(R2UploadService r2UploadService) {
        this.r2UploadService = r2UploadService;
    }

    public String saveSnapshot(Map<String, Object> snapshot) {

        String symbol = String.valueOf(snapshot.get("symbol"));

        if (symbol.isBlank() || "null".equals(symbol)) {
            throw new IllegalArgumentException("Missing option chain symbol");
        }

        String objectKey = "option-chain-nifty.json";

        // Don't overwrite valid existing data if every API call failed.
        Object expiriesObject = snapshot.get("expiries");

        if (!(expiriesObject instanceof List<?> expiries)
                || expiries.isEmpty()) {
            throw new IllegalArgumentException("No expiry data available");
        }

        boolean hasSuccessfulExpiry = expiries.stream()
                .anyMatch(expiry ->
                        expiry instanceof Map<?, ?> expiryMap
                                && "SUCCESS".equals(expiryMap.get("status"))
                );

        if (!hasSuccessfulExpiry) {
            throw new RuntimeException(
                    "All option chain API calls failed. Skipping R2 upload."
            );
        }

        Path tempFile = null;

        try {
            tempFile = Files.createTempFile("option-chain-", ".json");

            byte[] jsonData = objectMapper.writeValueAsBytes(snapshot);

            Files.write(tempFile, jsonData);

            // Same key every time: previous data gets overwritten.
            r2UploadService.upload(tempFile, objectKey);

            log.info("Updated latest option chain data: {}", objectKey);

            return objectKey;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to update option chain snapshot for " + symbol, e
            );
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("Could not delete temporary file: {}", tempFile);
                }
            }
        }
    }

}
