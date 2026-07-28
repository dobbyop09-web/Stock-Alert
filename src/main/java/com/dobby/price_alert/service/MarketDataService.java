package com.dobby.price_alert.service;

import com.dobby.price_alert.client.NSEClient;
import com.dobby.price_alert.dto.MarketData;
import com.dobby.price_alert.dto.nse.EquityResponse;
import com.dobby.price_alert.dto.nse.MetaData;
import com.dobby.price_alert.dto.nse.NseResponse;
import com.dobby.price_alert.dto.nse.TradeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

        return new MarketData(
                tradeInfo.getLastPrice().doubleValue(),
                tradeInfo.getTotalMarketCap().doubleValue(),
                metaData.getPreviousClose().doubleValue(),
                metaData.getPChange().doubleValue()
        );
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
