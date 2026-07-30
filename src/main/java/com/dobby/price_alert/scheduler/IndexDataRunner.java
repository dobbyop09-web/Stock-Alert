package com.dobby.price_alert.scheduler;

import com.dobby.price_alert.client.NSEClient;
import com.dobby.price_alert.dto.nse.IndexData;
import com.dobby.price_alert.dto.nse.IndexResponseData;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tools.jackson.databind.SerializationFeature;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class IndexDataRunner implements CommandLineRunner {
    @Autowired
    private NSEClient client;

    private static final List<String> DASHBOARD_INDICES = List.of(
            "NIFTY 50",
            "NIFTY BANK",
            "NIFTY SMALLCAP 250",
            "NIFTY AUTO",
            "NIFTY FMCG",
            "NIFTY PSU BANK",
            "NIFTY ENERGY",
            "NIFTY METAL",
            "NIFTY OIL & GAS",
            "NIFTY HEALTHCARE INDEX",
            "NIFTY INDIA DEFENCE",
            "NIFTY IT",
            "NIFTY CAPITAL MARKETS"
    );
    @Override
    public void run(String... args) throws Exception {
        IndexResponseData response = client.getIndexData();
        Map<String, IndexData> indexMap = response.getData().stream()
                .collect(Collectors.toMap(
                        IndexData::getIndex,
                        Function.identity()
                ));

        List<IndexData> dashboardIndices = DASHBOARD_INDICES.stream()
                .map(indexMap::get)
                .filter(Objects::nonNull)
                .toList();

        ObjectMapper mapper = new ObjectMapper();

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(
                        new File("dashboard/dashboard-indices.json"),
                        dashboardIndices
                );
    }

}
