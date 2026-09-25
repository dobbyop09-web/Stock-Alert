package com.dobby.price_alert.service.kotak;

import com.dobby.price_alert.dto.KotakScrip;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class KotakScripLookupService {
    private final KotakScripMasterApiService apiService;
    private final KotakScripMasterParser parser;
    private final KotakScripMasterApiService kotakScripMasterApiService;
    private final KotakScripMasterParser scripMasterParser;

    public KotakScripLookupService(
            KotakScripMasterApiService apiService,
            KotakScripMasterParser parser, KotakScripMasterApiService kotakScripMasterApiService, KotakScripMasterParser scripMasterParser) {

        this.apiService = apiService;
        this.parser = parser;
        this.kotakScripMasterApiService = kotakScripMasterApiService;
        this.scripMasterParser = scripMasterParser;
    }

    public Map<String, KotakScrip> buildNseScripMap(
            String nseMasterUrl) {

        String csv =
                apiService.downloadNseCashMaster(
                        nseMasterUrl
                );

        List<KotakScrip> scrips =
                parser.parse(csv);

        Map<String, KotakScrip> result =
                new HashMap<>();

        for (KotakScrip scrip : scrips) {

            if (!"nse_cm".equals(
                    scrip.getExchangeSegment())) {
                continue;
            }

            if (scrip.getSymbolName() == null ||
                    scrip.getSymbolName().isBlank()) {
                continue;
            }

            result.put(
                    scrip.getSymbolName(),
                    scrip
            );
        }

        return result;
    }

    public Map<String, KotakScrip> findMatchingScrips(
            List<String> symbols,
            Map<String, KotakScrip> scripMap) {

        Map<String, KotakScrip> matched =
                new HashMap<>();

        for (String symbol : symbols) {

            KotakScrip scrip =
                    scripMap.get(symbol);

            if (scrip != null) {
                matched.put(symbol, scrip);
            } else {
                System.out.println(
                        "Kotak scrip not found: " + symbol
                );
            }
        }

        return matched;
    }
    public Map<String, KotakScrip> loadNseScripMap() {

        String nseMaster;

        try {
            String today = LocalDate.now().toString();
            nseMaster =
                    kotakScripMasterApiService.downloadNseCashMaster(
                            "https://lapi.kotaksecurities.com/wso2-scripmaster/v1/prod/"+today+"/transformed-v1/nse_cm-v1.csv"
                    );

            log.info("NSE master downloaded from Kotak API.");

        } catch (Exception e) {

            log.warn(
                    "Unable to download NSE master from Kotak. "
                            + "Using local fallback file.",
                    e
            );

            nseMaster = loadLocalNseMaster();
        }

        List<KotakScrip> scrips =
                scripMasterParser.parse(nseMaster);

        return buildScripMap(scrips);
    }
    private String loadLocalNseMaster() {

        try {

            ClassPathResource resource =
                    new ClassPathResource("nse_cm-v1.csv");

            try (InputStream inputStream =
                         resource.getInputStream()) {

                return new String(
                        inputStream.readAllBytes(),
                        StandardCharsets.UTF_8
                );
            }

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Unable to load local NSE scrip master",
                    e
            );
        }
    }
    private Map<String, KotakScrip> buildScripMap(
            List<KotakScrip> scrips) {

        Map<String, KotakScrip> scripMap =
                new HashMap<>();
        scripMap.put("NSE",new KotakScrip("544937","bse_cm","NSE","NSE-EQ","INE721I01024"));

        for (KotakScrip scrip : scrips) {

            if (!"nse_cm".equals(
                    scrip.getExchangeSegment())) {
                continue;
            }

            if (scrip.getSymbolName() == null ||
                    scrip.getSymbolName().isBlank()) {
                continue;
            }

            scripMap.put(
                    scrip.getSymbolName(),
                    scrip
            );
        }

        log.info(
                "Loaded {} NSE Kotak scrips",
                scripMap.size()
        );

        return scripMap;
    }


}
