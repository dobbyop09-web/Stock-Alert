package com.dobby.price_alert.runners;

import com.dobby.price_alert.dto.nse.derivatives.DerivativesStockList;
import com.dobby.price_alert.service.DerivativesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Component
@Order(4)
public class DerivativesRunner implements CommandLineRunner {

    private final ObjectMapper objectMapper;

    private final DerivativesService service;

    public DerivativesRunner(ObjectMapper objectMapper, DerivativesService service) {
        this.objectMapper = objectMapper;
        this.service = service;
    }

    @Override
    public void run(String... args) throws Exception {

        ClassPathResource resource =
                new ClassPathResource("future-stocks.json");

        try (InputStream inputStream = resource.getInputStream()) {

            DerivativesStockList stockList =
                    objectMapper.readValue(
                            inputStream,
                            DerivativesStockList.class
                    );

            Set<String> symbols =
                    new LinkedHashSet<>(stockList.getStocks());

            log.info(
                    "Loaded {} unique derivative stocks",
                    symbols.size()
            );

            service.fetchAndStoreDerivativesData(symbols);
        }
    }
}