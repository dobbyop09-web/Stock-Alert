package com.dobby.price_alert.service;

import com.dobby.price_alert.dto.*;
import com.dobby.price_alert.service.kotak.KotakBatchMarketDataService;
import com.dobby.price_alert.service.kotak.KotakMarketDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.dobby.price_alert.dto.KotakScrip;
import com.dobby.price_alert.service.kotak.KotakScripLookupService;


import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
public class JsonStockConfigService {

    private final StockConfigService stockConfigService;
    private final MarketDataService marketDataService;
    private final KotakMarketDataService kotakMarketDataService;
    private final StockProcessingService stockProcessingService;
    private final KotakScripLookupService kotakScripLookupService;
    private final KotakBatchMarketDataService kotakBatchMarketDataService;
    private final MarketDataToggleService marketDataToggleService;

    public JsonStockConfigService(
            StockConfigService stockConfigService,
            MarketDataService marketDataService,
            KotakMarketDataService kotakMarketDataService,
            MarketDataToggleService marketDataToggleService,
            KotakScripLookupService kotakScripLookupService,
            KotakBatchMarketDataService kotakBatchMarketDataService,
            StockProcessingService stockProcessingService
    ) {
        this.stockConfigService = stockConfigService;
        this.marketDataService = marketDataService;
        this.kotakMarketDataService = kotakMarketDataService;
        this.marketDataToggleService = marketDataToggleService;
        this.kotakScripLookupService = kotakScripLookupService;
        this.kotakBatchMarketDataService = kotakBatchMarketDataService;
        this.stockProcessingService = stockProcessingService;
    }

    public List<DashboardStock> processStocks(Set<String> triggeredToday) {

        List<StockConfig> stockConfigs =
                stockConfigService.loadStockConfig();

        stockConfigs.sort(
                Comparator.comparing(StockConfig::getSheet)
        );

        String marketDataProvider =
                marketDataToggleService.getMarketDataProvider();

        log.info(
                "JSON market data provider: {}",
                marketDataProvider
        );

        Map<String, KotakScrip> scripMap =
                new HashMap<>();

        if ("KOTAK".equalsIgnoreCase(marketDataProvider)) {

            scripMap =
                    kotakScripLookupService.loadNseScripMap();

            log.info(
                    "Kotak scrip map loaded: {} symbols",
                    scripMap.size()
            );
        }

        List<DashboardStock> dashboardStocks =
                new ArrayList<>();

        int index = 0;

        while (index < stockConfigs.size()) {

            String sheet =
                    stockConfigs.get(index).getSheet();

            log.info(
                    "Reading {}",
                    sheet
            );

            int endIndex = index;

            while (endIndex < stockConfigs.size()
                    && sheet.equals(
                    stockConfigs.get(endIndex).getSheet())) {

                endIndex++;
            }

            List<StockConfig> sheetStocks =
                    stockConfigs.subList(
                            index,
                            endIndex
                    );

            Map<String, MarketData> marketDataMap = new HashMap<>();

            if ("KOTAK".equalsIgnoreCase(marketDataProvider)) {

                List<String> symbols =
                        sheetStocks.stream()
                                .map(StockConfig::getSymbol)
                                .filter(symbol ->
                                        symbol != null
                                                && !symbol.isBlank()
                                )
                                .toList();

                marketDataMap = kotakBatchMarketDataService.getMarketData(symbols, scripMap);
            }

            for (StockConfig config : sheetStocks) {

                String symbol =
                        config.getSymbol();

                String companyName =
                        config.getCompanyName();

                BigDecimal alertPrice =
                        config.getAlertPrice();

                MarketData marketData;

                if ("KOTAK".equalsIgnoreCase(marketDataProvider)) {

                    if ("NSE".equals(symbol)) {

                        marketData =
                                kotakMarketDataService
                                        .getMarketData("544937");

                    } else {

                        marketData =
                                marketDataMap.get(symbol);
                    }

                } else {

                    if ("NSE".equals(symbol)) {

                        marketData =
                                kotakMarketDataService
                                        .getMarketData("544937");

                    } else {

                        marketData =
                                marketDataService
                                        .getMarketData(symbol);
                    }
                }

                if (marketData == null) {

                    log.warn(
                            "No market data found for {}",
                            symbol
                    );

                    continue;
                }

                DashboardStock dashboardStock =
                        stockProcessingService.processStock(
                                symbol,
                                companyName,
                                sheet,
                                alertPrice,
                                marketData,
                                triggeredToday,
                                null,
                                null
                        );

                dashboardStocks.add(
                        dashboardStock
                );
            }

            index = endIndex;
        }

        return dashboardStocks;
    }
}