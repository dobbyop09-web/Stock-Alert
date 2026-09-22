package com.dobby.price_alert.service;

import com.dobby.price_alert.client.NSEClient;
import com.dobby.price_alert.dto.nse.derivatives.DerivativesDataDto;
import com.dobby.price_alert.dto.nse.derivatives.DerivativesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class DerivativesService {
    @Autowired
    private NSEClient client;
    @Autowired
    private R2UploadService r2UploadService;

    public void fetchAndStoreDerivativesData(Set<String> symbols) throws IOException {
        Map<String, List<DerivativesDataDto>> derivativesBySymbol =
                new LinkedHashMap<>();

        for (String symbol : symbols) {
            try {
                DerivativesResponse response = client.getFutureData(symbol);
                // Store the derivatives data as needed
                if (response == null || response.getData() == null) {
                    log.warn("No derivatives data received for {}", symbol);
                    continue;
                }
                Map<String, DerivativesDataDto> uniqueContracts =
                        new LinkedHashMap<>();

                for (DerivativesDataDto data : response.getData()) {
                    String identifier = data.getIdentifier();
                    if (identifier == null || identifier.isBlank()) {
                        continue;
                    }
                    // Last record wins if NSE returns duplicate identifier
                    uniqueContracts.put(identifier, data);
                }
                derivativesBySymbol.put(
                        symbol,
                        new ArrayList<>(uniqueContracts.values())
                );
                log.debug(
                        "Fetched {} derivative contracts for {}",
                        uniqueContracts.size(),
                        symbol
                );
            } catch (Exception e) {
                log.error("Error fetching derivatives data for {}: {}", symbol, e.getMessage());
            }
        }

        Map<String, Object> output = new LinkedHashMap<>();

        output.put(
                "fetchedAt",
                LocalDateTime.now().toString()
        );

        output.put(
                "data",
                derivativesBySymbol
        );

        log.info(
                "Fetched derivatives data for {} symbols",
                derivativesBySymbol.size()
        );

        /*
         * Write ONE JSON file
         */
        ObjectMapper mapper = new ObjectMapper();

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(
                        new File("derivatives-data.json"),
                        output
                );

        /*
         * Upload ONE file to R2
         */
        r2UploadService.upload(
                Path.of("derivatives-data.json"),
                "derivatives-data.json"
        );

        log.info(
                "Derivatives data written and uploaded successfully. Symbols: {}",
                derivativesBySymbol.size()
        );
    }
}
