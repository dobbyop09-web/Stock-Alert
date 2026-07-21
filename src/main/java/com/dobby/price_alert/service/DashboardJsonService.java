package com.dobby.price_alert.service;

import com.dobby.price_alert.dto.DashboardStock;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class DashboardJsonService {
    private final ObjectMapper mapper = new ObjectMapper();

    public void write(List<DashboardStock> stocks) throws IOException {

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(
                        new File("dashboard/dashboard-data.json"),
                        stocks
                );
    }
}
