package com.dobby.price_alert.runners;

import com.dobby.price_alert.client.NSEClient;
import com.dobby.price_alert.constants.DashboardIndex;
import com.dobby.price_alert.dto.nse.index.DashboardIndexData;
import com.dobby.price_alert.dto.nse.index.IndexData;
import com.dobby.price_alert.dto.nse.index.IndexResponseData;
import com.dobby.price_alert.mapper.DashboardIndexMapper;
import com.dobby.price_alert.service.R2UploadService;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class IndexDataRunner implements CommandLineRunner {
    @Autowired
    private NSEClient client;

    @Autowired
    private R2UploadService r2UploadService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Fetching index data...");

        IndexResponseData response = client.getIndexData();

        Map<String, IndexData> indexMap =
                response.getData()
                        .stream()
                        .collect(Collectors.toMap(
                                IndexData::getIndex,
                                Function.identity()
                        ));

        List<DashboardIndexData> dashboardIndices =
                Arrays.stream(DashboardIndex.values())
                        .map(config -> {
                            IndexData data = indexMap.get(config.getNseName());

                            if (data == null) {
                                return null;
                            }

                            return DashboardIndexMapper.map(data);
                        })
                        .filter(Objects::nonNull)
                        .toList();

        ObjectMapper mapper = new ObjectMapper();

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(
                        new File("dashboard/dashboard-indices.json"),
                        dashboardIndices
                );
        r2UploadService.upload(
                Path.of("dashboard/dashboard-indices.json"),
                "dashboard/dashboard-indices.json");
        log.info("Dashboard index data written successfully.");
    }
}


