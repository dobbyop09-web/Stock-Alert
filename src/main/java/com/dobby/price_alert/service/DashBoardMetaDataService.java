package com.dobby.price_alert.service;

import com.dobby.price_alert.dto.DashboardMetaData;
import com.dobby.price_alert.dto.DashboardStock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DashBoardMetaDataService {
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private R2UploadService r2UploadService;

    private void write(DashboardMetaData metaData) throws IOException {

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(
                        new File("dashboard/dashboard-meta.json"),
                        metaData
                );
        r2UploadService.upload(
                Path.of("dashboard/dashboard-meta.json"),
                "dashboard/dashboard-meta.json");
    }

    private DashboardMetaData build(long generationTimeMs) {

        return DashboardMetaData.builder()
                .lastUpdated(
                        ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                )
                .generationTimeMs(generationTimeMs)
                .build();
    }
    public void buildAndWrite(long generationTimeMs) throws IOException {

        DashboardMetaData metaData = build(generationTimeMs);
        write(metaData);
    }
}
