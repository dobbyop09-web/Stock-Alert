package com.dobby.price_alert.service;

import com.dobby.price_alert.client.NSEClient;
import com.dobby.price_alert.dto.*;
import com.dobby.price_alert.dto.nse.EquityResponse;
import com.dobby.price_alert.dto.nse.MetaData;
import com.dobby.price_alert.dto.nse.NseResponse;
import com.dobby.price_alert.dto.nse.TradeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
public class MarketDataService {

    @Autowired
    private NSEClient nseClient;

    @Autowired
    private ExecutorService marketDataExecutor;

    @Autowired
    private StockAlertService stockAlertService;

    @Autowired
    private TelegramService telegramService;

    private MarketData getMarketData(String symbol) {
        NseResponse response = nseClient.getPriceDetails(symbol, "EQ");

        if (!isValidResponse(response)) {
            response = nseClient.getPriceDetails(symbol, "BE");
        }

        if (!isValidResponse(response)) {
            throw new RuntimeException("No market data returned for symbol " + symbol);
        }

        EquityResponse equity = response.getEquityResponse().get(0);
        MetaData metaData = equity.getMetaData();
        TradeInfo tradeInfo = equity.getTradeInfo();
        BigDecimal currentPrice = (metaData.getClosePrice() == null
                || BigDecimal.ZERO.compareTo(metaData.getClosePrice()) == 0)
                ? tradeInfo.getLastPrice()
                : metaData.getClosePrice();
        return MarketData.builder()
                .currentPrice(currentPrice.doubleValue())
                .previousPrice(metaData.getPreviousClose().doubleValue())
                .marketCap(tradeInfo.getTotalMarketCap().doubleValue())
                .changePercent(metaData.getPChange().doubleValue())
                .build();
    }
    private Map<String,MarketData> getMarketData(List<CsvRecordDto> records) {
        log.info("Fetching market data for {} symbols", records.size());
            List<CompletableFuture<Map.Entry<String, MarketData>>> futures =
                    records.stream()
                            .map(record ->
                                    CompletableFuture.supplyAsync(() -> {
                                        log.debug("Thread {} fetching {}",
                                                Thread.currentThread().getName(),
                                                record.getSymbol());

                                        try {
                                            return Map.entry(record.getSymbol(), getMarketData(record.getSymbol()));
                                        } catch (Exception ex) {
                                            log.error("Failed to fetch {}", record.getSymbol(), ex);
                                            return null;
                                        }

                                    }, marketDataExecutor)
                            )
                            .toList();

        Map<String, MarketData> result = new HashMap<>();

        for (CompletableFuture<Map.Entry<String, MarketData>> future : futures) {

            Map.Entry<String, MarketData> entry =
                    future.join();

            if (entry != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private boolean isValidResponse(NseResponse response) {
        return response != null
                && response.getEquityResponse() != null
                && !response.getEquityResponse().isEmpty()
                && response.getEquityResponse().get(0) != null
                && response.getEquityResponse().get(0).getMetaData() != null
                && response.getEquityResponse().get(0).getTradeInfo() != null;
    }
    public List<DashboardStock> getDashboardData(List<CsvRecordDto> recordList, Set<String> triggeredToday) {
        List<DashboardStock> dashboardStocks = new ArrayList<>();
        Map<String,MarketData> marketDataMap = this.getMarketData(recordList);
        for(CsvRecordDto record : recordList) {
            MarketData marketData = marketDataMap.get(record.getSymbol());
            if(marketData == null) {
                log.warn("No market data returned for symbol {}", record.getSymbol());
                continue;
            }
            String symbol = record.getSymbol();
            double alert = record.getAlertPrice();
            double fib = record.getFib();
            int rowNumber = record.getRowNumber();

            double current =marketData.getCurrentPrice();
            double prevClose = marketData.getPreviousPrice();
            double marketCap = marketData.getMarketCap();

            String screenerUrl ="https://www.screener.in/company/" + symbol + "/consolidated/";
            AlertStatus alertStatus = stockAlertService.shouldSendAlert(record.getSheetName(), symbol, current, alert,triggeredToday);
            if (alertStatus.isShouldSend()) {
                log.info("Telegram sent for {}", symbol);

                StockMessageDto dto = StockMessageDto.builder().stockName(symbol).currentPrice(current).targetPrice(alert).screenerUrl(screenerUrl).sheetName(record.getSheetName()).build();
                String message = MessageFormat.format(dto);
                telegramService.sendMessage(message);

            }
            double distance = ((current - alert) / alert) * 100;

            double changePerc = ((current-prevClose)/prevClose)*100;
            String status;

            if (alertStatus.isTriggeredToday()) {
                status = "Triggered";
            } else if (distance <= 5) {
                status = "Near";
            } else if (distance <= 15) {
                status = "Watch";
            } else {
                status = "Far";
            }

            dashboardStocks.add(
                            DashboardStock.builder()
                                    .symbol(symbol)
                                    .currentPrice(BigDecimal.valueOf(current))
                                    .alertPrice(BigDecimal.valueOf(alert))
                                    .distance(BigDecimal.valueOf(distance))
                                    .status(status)
                                    .sheet(record.getSheetName())
                    .previousClose(BigDecimal.valueOf(prevClose))
                    .marketCap(BigDecimal.valueOf(marketCap))
                    .changePercent(BigDecimal.valueOf(changePerc))
                    .sheetRow(rowNumber)
                    .screenerUrl(screenerUrl)
                    .fib(BigDecimal.valueOf(fib))
                    .build()
            );
        }
        return dashboardStocks;
    }
}
