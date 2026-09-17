package com.dobby.price_alert.service;

import com.dobby.price_alert.dto.AlertHistory;
import com.dobby.price_alert.dto.HistoricalAlert;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;

@Service
public class AlertHistoryService {

    private static final String ALERT_HISTORY_OBJECT_KEY =
            "alert-history.json";

    private final R2UploadService r2UploadService;
    private final ObjectMapper objectMapper;

    public AlertHistoryService(
            R2UploadService r2UploadService,
            ObjectMapper objectMapper
    ) {
        this.r2UploadService = r2UploadService;
        this.objectMapper = objectMapper;
    }


    /*
     * ============================================================
     * LOAD ALERT HISTORY
     * ============================================================
     */

    public AlertHistory loadAlertHistory() {

        Path historyFile =
                r2UploadService.download(
                        ALERT_HISTORY_OBJECT_KEY
                );


        /*
         * First run:
         * alert-history.json does not exist yet.
         */
        if (historyFile == null) {

            return AlertHistory.builder()
                    .alerts(new ArrayList<>())
                    .build();
        }


        try {

            AlertHistory alertHistory =
                    objectMapper.readValue(
                            Files.readString(historyFile),
                            AlertHistory.class
                    );


            Files.deleteIfExists(historyFile);


            /*
             * Safety in case JSON has:
             *
             * "alerts": null
             */
            if (alertHistory.getAlerts() == null) {

                alertHistory.setAlerts(
                        new ArrayList<>()
                );
            }


            return alertHistory;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to read alert history from R2",
                    e
            );
        }
    }


    /*
     * ============================================================
     * ADD NEW ALERT
     * ============================================================
     */

    public void addAlert(
            HistoricalAlert historicalAlert
    ) {

        AlertHistory alertHistory =
                loadAlertHistory();


        alertHistory.getAlerts()
                .add(historicalAlert);


        alertHistory.setLastUpdated(
                Instant.now().toString()
        );


        saveAlertHistory(alertHistory);
    }


    /*
     * ============================================================
     * SAVE ALERT HISTORY
     * ============================================================
     */

    public void saveAlertHistory(
            AlertHistory alertHistory
    ) {

        Path tempFile = null;

        try {

            tempFile =
                    Files.createTempFile(
                            "alert-history-",
                            ".json"
                    );


            objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(
                            tempFile.toFile(),
                            alertHistory
                    );


            r2UploadService.upload(
                    tempFile,
                    ALERT_HISTORY_OBJECT_KEY
            );


        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to save alert history to R2",
                    e
            );

        } finally {

            if (tempFile != null) {

                try {

                    Files.deleteIfExists(tempFile);

                } catch (Exception ignored) {
                    // Nothing else to do.
                }
            }
        }
    }
}