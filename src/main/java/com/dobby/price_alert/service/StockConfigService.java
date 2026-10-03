package com.dobby.price_alert.service;

import com.dobby.price_alert.dto.StockConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class StockConfigService {

    private static final String STOCK_CONFIG_KEY = "stock-config.json";

    private final R2UploadService r2UploadService;
    private final ObjectMapper objectMapper;

    public StockConfigService(
            R2UploadService r2UploadService,
            ObjectMapper objectMapper
    ) {
        this.r2UploadService = r2UploadService;
        this.objectMapper = objectMapper;
    }

    public List<StockConfig> loadStockConfig() {

        Path tempFile = r2UploadService.download(STOCK_CONFIG_KEY);

        if (tempFile == null) {
            throw new RuntimeException(
                    "stock-config.json not found in R2"
            );
        }

        try {
            return objectMapper.readValue(
                    tempFile.toFile(),
                    new TypeReference<List<StockConfig>>() {}
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to read stock-config.json",
                    e
            );

        } finally {

            try {
                Files.deleteIfExists(tempFile);
            } catch (Exception ignored) {
                // Ignore cleanup failure
            }
        }
    }
}