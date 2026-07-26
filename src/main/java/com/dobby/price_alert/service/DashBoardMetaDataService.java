package com.dobby.price_alert.service;

import com.dobby.price_alert.dto.DashboardMetaData;
import com.dobby.price_alert.dto.DashboardStock;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DashBoardMetaDataService {
    private final ObjectMapper mapper = new ObjectMapper();

    private void write(DashboardMetaData metaData) throws IOException {

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(
                        new File("dashboard/dashboard-meta.json"),
                        metaData
                );
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
        System.out.print(metaData.toString());
        write(metaData);
    }
}
