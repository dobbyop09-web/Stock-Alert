package com.dobby.price_alert.service.nse;

import com.dobby.price_alert.client.NSEClient;
import com.dobby.price_alert.service.R2UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
public class VixHistoryService {

    private final NSEClient nseClient;
    private final R2UploadService r2UploadService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String R2_FILE =
            "vix-history.json";

    private static final ZoneId ZONE =
            ZoneId.of("Asia/Kolkata");

    private static final DateTimeFormatter VIX_DATE_FORMAT =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("dd-MMM-yyyy")
                    .toFormatter(Locale.ENGLISH);

    public VixHistoryService(
            NSEClient nseClient,
            R2UploadService r2UploadService) {

        this.nseClient = nseClient;
        this.r2UploadService = r2UploadService;
    }

    public void updateVixHistory() {

        LocalDate today = LocalDate.now(ZONE);

        Path existingFile = null;
        Path uploadFile = null;

        try {

            // Step 1: Download existing VIX history
            existingFile = r2UploadService.download(R2_FILE);

            List<Map<String, Object>> existingRecords =
                    new ArrayList<>();

            LocalDate fromDate;

            if (existingFile == null) {

                // First execution: fetch previous one year
                fromDate = today.minusYears(1);

                log.info(
                        "No existing VIX history found. Initial backfill from {} to {}",
                        fromDate, today
                );

            } else {

                existingRecords = readExistingRecords(existingFile);

                if (existingRecords.isEmpty()) {

                    // Empty existing file: perform initial backfill
                    fromDate = today.minusYears(1);

                } else {

                    // Find latest date already stored
                    LocalDate latestStoredDate =
                            findLatestDate(existingRecords);

                    if (latestStoredDate.isAfter(today)) {
                        throw new IllegalStateException(
                                "Latest stored VIX date is in the future: "
                                        + latestStoredDate
                        );
                    }

                    // Start from latest date to refresh it
                    fromDate = latestStoredDate;

                    log.info(
                            "Existing VIX history found. Latest date: {}",
                            latestStoredDate
                    );
                }
            }

            // Step 2: Fetch missing VIX records from NSE
            log.info(
                    "Fetching VIX records from {} to {}",
                    fromDate, today
            );

            List<Map<String, Object>> newRecords =
                    nseClient.getVixHistory(fromDate, today);

            if (newRecords == null) {
                throw new RuntimeException(
                        "NSE returned null VIX response"
                );
            }

            log.info(
                    "Received {} VIX records from NSE",
                    newRecords.size()
            );

            // Step 3: Do not overwrite anything with an empty response
            if (newRecords.isEmpty()) {

                if (existingRecords.isEmpty()) {
                    throw new RuntimeException(
                            "Initial VIX history fetch returned no records"
                    );
                }

                log.info(
                        "No new VIX records available. Existing R2 file remains unchanged."
                );

                return;
            }

            // Step 4: Merge old and new records using trading date
            TreeMap<LocalDate, Map<String, Object>> mergedData =
                    new TreeMap<>();

            for (Map<String, Object> record : existingRecords) {

                LocalDate date = getRecordDate(record);

                mergedData.put(date, record);
            }

            for (Map<String, Object> record : newRecords) {

                LocalDate date = getRecordDate(record);

                // New NSE record replaces an existing record
                mergedData.put(date, record);
            }

            // Step 5: Convert sorted records back to a list
            List<Map<String, Object>> finalRecords =
                    new ArrayList<>(mergedData.values());

            log.info(
                    "Merged VIX history. Total records: {}",
                    finalRecords.size()
            );

            // Step 6: Create temporary JSON file
            uploadFile = Files.createTempFile(
                    "vix-history-",
                    ".json"
            );

            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(
                            uploadFile.toFile(),
                            finalRecords
                    );

            // Step 7: Upload to R2
            r2UploadService.upload(
                    uploadFile,
                    R2_FILE
            );

            log.info(
                    "VIX history successfully updated in R2. Total records: {}",
                    finalRecords.size()
            );

        } catch (Exception e) {

            log.error(
                    "Failed to update VIX history: {}",
                    e.getMessage(),
                    e
            );

            throw new RuntimeException(
                    "VIX history update failed",
                    e
            );

        } finally {

            // Cleanup downloaded temporary file
            deleteTempFile(existingFile);

            // Cleanup upload temporary file
            deleteTempFile(uploadFile);
        }
    }

    private List<Map<String, Object>> readExistingRecords(
            Path file) throws IOException {

        // Support both a direct JSON array and the earlier
        // { "data": [...] } response structure.
        Object existingJson = objectMapper.readValue(
                file.toFile(),
                Object.class
        );

        Object rows = existingJson;

        if (existingJson instanceof Map<?, ?> wrapper) {
            rows = wrapper.get("data");
        }

        if (!(rows instanceof List<?> list)) {
            throw new IllegalStateException(
                    "Invalid VIX history JSON structure in R2"
            );
        }

        List<Map<String, Object>> records = new ArrayList<>();

        for (Object row : list) {

            if (!(row instanceof Map<?, ?> map)) {
                throw new IllegalStateException(
                        "Invalid VIX history record in R2"
                );
            }

            Map<String, Object> record = new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {

                if (!(entry.getKey() instanceof String key)) {
                    throw new IllegalStateException(
                            "Invalid field name in VIX history record"
                    );
                }

                record.put(key, entry.getValue());
            }

            records.add(record);
        }

        return records;
    }

    private LocalDate findLatestDate(
            List<Map<String, Object>> records) {

        return records.stream()
                .map(this::getRecordDate)
                .max(LocalDate::compareTo)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No valid dates found in VIX history"
                        )
                );
    }

    private LocalDate getRecordDate(
            Map<String, Object> record) {

        Object timestamp = record.get("EOD_TIMESTAMP");

        if (timestamp == null) {
            throw new IllegalStateException(
                    "EOD_TIMESTAMP missing in VIX record"
            );
        }

        return LocalDate.parse(
                timestamp.toString().trim(),
                VIX_DATE_FORMAT
        );
    }

    private void deleteTempFile(Path file) {

        if (file == null) {
            return;
        }

        try {
            Files.deleteIfExists(file);

        } catch (IOException e) {

            log.warn(
                    "Could not delete temporary file: {}",
                    file
            );
        }
    }
}
