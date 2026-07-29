package com.dobby.price_alert.service;

import com.dobby.price_alert.client.NSEClient;
import com.dobby.price_alert.dto.MarketData;
import com.dobby.price_alert.dto.nse.EquityResponse;
import com.dobby.price_alert.dto.nse.MetaData;
import com.dobby.price_alert.dto.nse.NseResponse;
import com.dobby.price_alert.dto.nse.TradeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.error.Mark;

import java.math.BigDecimal;

@Service
public class MarketDataService {

    @Autowired
    private NSEClient nseClient;

    public MarketData getMarketData(String symbol) {
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
                .marketCap(tradeInfo.getTotalMarketCap().doubleValue())
                .previousPrice(metaData.getPreviousClose().doubleValue())
                .changePercent(metaData.getPChange().doubleValue())
                .dayLow(metaData.getDayLow().doubleValue())
                .build();
    }

    private boolean isValidResponse(NseResponse response) {
        return response != null
                && response.getEquityResponse() != null
                && !response.getEquityResponse().isEmpty()
                && response.getEquityResponse().get(0) != null
                && response.getEquityResponse().get(0).getMetaData() != null
                && response.getEquityResponse().get(0).getTradeInfo() != null;
    }
}
