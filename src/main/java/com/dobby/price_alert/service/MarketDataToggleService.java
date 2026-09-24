package com.dobby.price_alert.service;

import com.dobby.price_alert.dto.MarketDataToggle;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class MarketDataToggleService {

    private final R2UploadService r2UploadService;
    private final ObjectMapper objectMapper;

    private static final String TOGGLE_FILE =
            "toggle.json";

    public String getMarketDataProvider() {

        Path file =
                r2UploadService.download(TOGGLE_FILE);

        if (file == null) {

            /*
             * If toggle.json doesn't exist,
             * keep the existing NSE behaviour.
             */
            return "KOTAK";
        }

        try {

            MarketDataToggle toggle =
                    objectMapper.readValue(
                            Files.readAllBytes(file),
                            MarketDataToggle.class
                    );

            String provider =
                    toggle.getMarketDataProvider();

            if (provider == null
                    || provider.isBlank()) {

                return "KOTAK";
            }

            return provider.toUpperCase();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to read " + TOGGLE_FILE,
                    e
            );

        } finally {

            try {
                Files.deleteIfExists(file);
            } catch (Exception ignored) {
            }
        }
    }
}