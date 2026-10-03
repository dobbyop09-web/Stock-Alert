package com.dobby.price_alert.service.kotak;

import com.dobby.price_alert.dto.KotakScrip;
import com.dobby.price_alert.dto.MarketData;
import com.dobby.price_alert.dto.kotak.KotakQuoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KotakBatchMarketDataService {

    private static final Logger log =
            LoggerFactory.getLogger(KotakBatchMarketDataService.class);

    private final KotakQuoteService kotakQuoteService;
    private final KotakMarketDataService kotakMarketDataService;

    public KotakBatchMarketDataService(
            KotakQuoteService kotakQuoteService,
            KotakMarketDataService kotakMarketDataService
    ) {
        this.kotakQuoteService = kotakQuoteService;
        this.kotakMarketDataService = kotakMarketDataService;
    }

    public Map<String, MarketData> getMarketData(
            List<String> symbols,
            Map<String, KotakScrip> scripMap
    ) {

        Map<String, KotakScrip> matchedScrips =
                new HashMap<>();

        for (String symbol : symbols) {

            KotakScrip scrip =
                    scripMap.get(symbol);

            if (scrip != null) {

                matchedScrips.put(
                        symbol,
                        scrip
                );

            } else {

                log.warn(
                        "Kotak scrip not found for symbol: {}",
                        symbol
                );
            }
        }

        List<String> tokens =
                matchedScrips.values()
                        .stream()
                        .map(KotakScrip::getSymbol)
                        .toList();

        log.info(
                "Kotak symbols: {} | matched: {} | tokens: {}",
                symbols.size(),
                matchedScrips.size(),
                tokens.size()
        );

        if (tokens.isEmpty()) {
            return new HashMap<>();
        }

        List<KotakQuoteResponse> quotes =
                kotakQuoteService.getOhlc(
                        "nse_cm",
                        tokens
                );

        Map<String, MarketData> marketDataMap =
                new HashMap<>();

        for (KotakQuoteResponse quote : quotes) {

            String kotakSymbol =
                    quote.getDisplaySymbol();

            MarketData marketData =
                    kotakMarketDataService.mapToMarketData(quote);

            String symbol = kotakSymbol;

            int dashIndex =
                    symbol.lastIndexOf("-");

            if (dashIndex > 0) {
                symbol =
                        symbol.substring(
                                0,
                                dashIndex
                        );
            }

            marketDataMap.put(
                    symbol,
                    marketData
            );
        }

        return marketDataMap;
    }
}