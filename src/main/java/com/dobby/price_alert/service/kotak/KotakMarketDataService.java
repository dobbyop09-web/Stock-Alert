package com.dobby.price_alert.service.kotak;

import com.dobby.price_alert.dto.MarketData;
import com.dobby.price_alert.dto.kotak.KotakQuoteResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KotakMarketDataService {

    private final KotakQuoteService kotakQuoteService;

    public KotakMarketDataService(
            KotakQuoteService kotakQuoteService) {

        this.kotakQuoteService = kotakQuoteService;
    }

    public  MarketData getMarketData(String token) {
        KotakQuoteResponse quotes =
                kotakQuoteService.getQuote(
                        token
                );
        return mapToMarketData(quotes);
    }
    public MarketData mapToMarketData(
            KotakQuoteResponse quote) {

        if (quote == null) {
            return null;
        }

        double currentPrice =
                Double.parseDouble(
                        quote.getLtp()
                );

        double previousPrice =
                Double.parseDouble(
                        quote.getOhlc().getClose()
                );

        double dayLow =
                Double.parseDouble(
                        quote.getOhlc().getLow()
                );

        double changePercent =
                Double.parseDouble(
                        quote.getPercentageChange()
                );

        String companyName =
                quote.getDisplaySymbol();

        MarketData marketData =
                new MarketData();

        marketData.setCompanyName(
                companyName
        );

        marketData.setCurrentPrice(
                currentPrice
        );

        marketData.setPreviousPrice(
                previousPrice
        );

        marketData.setDayLow(
                dayLow
        );

        marketData.setChangePercent(
                changePercent
        );

        /*
         * Kotak quote response does not currently
         * provide market cap.
         */
        marketData.setMarketCap(0.0);

        return marketData;
    }
}
