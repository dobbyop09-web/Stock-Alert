package com.dobby.price_alert.service;

import com.dobby.price_alert.dto.DashboardStock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
public class DashboardJsonService {
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private R2UploadService r2UploadService;

    public void write(List<DashboardStock> stocks) throws IOException {

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(
                        new File("dashboard/dashboard-data.json"),
                        stocks
                );
        r2UploadService.upload(
                Path.of("dashboard/dashboard-data.json"),
                "dashboard/dashboard-data.json");
    }
}
