package com.dobby.price_alert.service;

import com.dobby.price_alert.client.NSEClient;
import com.dobby.price_alert.dto.nse.fii.FiiDataDto;
import com.dobby.price_alert.dto.nse.fii.FiiDiiHistoryDto;
import com.dobby.price_alert.dto.nse.fii.FiiDiiValuesDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FiiDataService {

    private static final String TODAY_FILE = "fii-dii.json";
    private static final String HISTORY_FILE = "fii-dii-history.json";

    @Autowired
    private NSEClient nseClient;

    @Autowired
    private R2UploadService r2UploadService;

    private final ObjectMapper objectMapper;


    public void updateFiiData() {

        log.info("Starting FII/DII update");

        List<FiiDataDto> latestData = nseClient.getFiiData();

        if (latestData == null || latestData.isEmpty()) {
            log.warn("NSE returned empty FII/DII data");
            return;
        }

        updateTodaySnapshot(latestData);
        updateHistory(latestData);

        log.info("FII/DII update completed successfully");
    }


    /**
     * Writes the latest NSE response to fii-dii.json.
     * Every execution overwrites the previous file.
     */
    private void updateTodaySnapshot(List<FiiDataDto> latestData) {

        Path outputFile = null;

        try {

            outputFile = Files.createTempFile("fii-dii-", ".json");

            objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(outputFile.toFile(), latestData);

            r2UploadService.upload(outputFile, TODAY_FILE);

        } catch (Exception e) {

            log.error("Failed to update today's FII/DII snapshot", e);
            throw new RuntimeException("Failed to update today's FII/DII snapshot", e);

        } finally {
            if (outputFile != null) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (Exception e) {
                    log.warn("Could not delete temporary today's FII/DII file", e);
                }
            }
        }
    }


    /**
     * Updates historical FII/DII data.
     * One entry per date — replaces if already exists.
     */
    private void updateHistory(List<FiiDataDto> latestData) {

        Path downloadedFile = null;
        Path outputFile = null;

        try {

            FiiDiiHistoryDto newEntry = createHistoryEntry(latestData);

            List<FiiDiiHistoryDto> history;

            try {
                downloadedFile = r2UploadService.download(HISTORY_FILE);

                if (downloadedFile != null) {
                    history = objectMapper.readValue(
                            downloadedFile.toFile(),
                            new TypeReference<List<FiiDiiHistoryDto>>() {}
                    );
                } else {
                    log.info("No existing FII/DII history found (first run). Starting fresh.");
                    history = new ArrayList<>();
                }

            } catch (Exception e) {
                log.warn("Could not parse existing FII/DII history. Starting fresh. Reason: {}", e.getMessage());
                history = new ArrayList<>();
            }

            // Remove existing entry for same date, add latest
            history.removeIf(entry ->
                    entry.getDate() != null && entry.getDate().equals(newEntry.getDate())
            );

            history.add(newEntry);

            // Sort newest first
            history.sort(Comparator.comparing(FiiDiiHistoryDto::getDate).reversed());

            outputFile = Files.createTempFile("fii-dii-history-", ".json");

            objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(outputFile.toFile(), history);

            r2UploadService.upload(outputFile, HISTORY_FILE);

        } catch (Exception e) {

            log.error("Failed to update FII/DII history", e);
            throw new RuntimeException("Failed to update FII/DII history", e);

        } finally {
            try {
                if (downloadedFile != null) Files.deleteIfExists(downloadedFile);
                if (outputFile != null) Files.deleteIfExists(outputFile);
            } catch (Exception e) {
                log.warn("Could not delete temporary FII/DII files", e);
            }
        }
    }


    private FiiDiiHistoryDto createHistoryEntry(List<FiiDataDto> data) {

        FiiDataDto fiiData = data.stream()
                .filter(item -> "FII/FPI".equalsIgnoreCase(item.getCategory()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("FII/FPI data not found"));

        FiiDataDto diiData = data.stream()
                .filter(item -> "DII".equalsIgnoreCase(item.getCategory()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("DII data not found"));

        FiiDiiHistoryDto history = new FiiDiiHistoryDto();
        history.setDate(fiiData.getDate());
        history.setFii(createValues(fiiData));
        history.setDii(createValues(diiData));

        return history;
    }


    private FiiDiiValuesDto createValues(FiiDataDto data) {

        FiiDiiValuesDto values = new FiiDiiValuesDto();
        values.setBuyValue(data.getBuyValue());
        values.setSellValue(data.getSellValue());
        values.setNetValue(data.getNetValue());

        return values;
    }
}