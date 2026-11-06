package com.dobby.price_alert.service;

import com.dobby.price_alert.dto.nse.derivatives.FiiIndexOiDto;
import com.dobby.price_alert.dto.nse.derivatives.FiiIndexOiHistoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class FiiIndexOiHistoryService {

    private static final String OBJECT_KEY = "fii-index-oi-history.json";

    private final R2UploadService r2UploadService;
    private final ObjectMapper objectMapper;

    public void saveOrUpdate(FiiIndexOiDto newEntry) {

        Path downloadedFile = null;
        Path uploadFile = null;

        try {
            FiiIndexOiHistoryDto history;

            // 1. Download existing history
            downloadedFile = r2UploadService.download(OBJECT_KEY);

            if (downloadedFile == null) {
                log.info("FII OI history file not found. Creating a new history.");
                history = new FiiIndexOiHistoryDto(new ArrayList<>());
            } else {
                String existingJson = Files.readString(downloadedFile);
                history = objectMapper.readValue(
                        existingJson,
                        FiiIndexOiHistoryDto.class
                );

                if (history.getHistory() == null) {
                    history.setHistory(new ArrayList<>());
                }
            }

            // 2. Check whether this date already exists
            boolean updated = false;

            for (int i = 0; i < history.getHistory().size(); i++) {

                if (history.getHistory().get(i).getDate()
                        .equals(newEntry.getDate())) {

                    history.getHistory().set(i, newEntry);
                    updated = true;
                    break;
                }
            }

            // 3. Append if date is new
            if (!updated) {
                history.getHistory().add(newEntry);
            }

            // 4. Serialize updated history
            history.getHistory().sort(
                    java.util.Comparator.comparing(FiiIndexOiDto::getDate)
            );
            String updatedJson = objectMapper.writeValueAsString(history);

            uploadFile = Files.createTempFile("fii-index-oi-", ".json");
            Files.writeString(uploadFile, updatedJson);

            // 5. Upload back to the same R2 object
            r2UploadService.upload(uploadFile, OBJECT_KEY);

            log.info(
                    "{} FII OI history for {}. Total entries: {}",
                    updated ? "Updated" : "Added",
                    newEntry.getDate(),
                    history.getHistory().size()
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to update FII Index OI history",
                    e
            );

        } finally {
            deleteTemporaryFile(downloadedFile);
            deleteTemporaryFile(uploadFile);
        }
    }

    private void deleteTemporaryFile(Path file) {
        if (file != null) {
            try {
                Files.deleteIfExists(file);
            } catch (Exception e) {
                log.warn("Could not delete temporary file: {}", file, e);
            }
        }
    }
}