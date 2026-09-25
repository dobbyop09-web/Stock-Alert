package com.dobby.price_alert.service.kotak;

import com.dobby.price_alert.dto.KotakScrip;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KotakScripMasterParser {

    public List<KotakScrip> parse(String csv) {

        List<KotakScrip> scrips = new ArrayList<>();

        String[] lines = csv.split("\\R");

        // Skip header
        for (int i = 1; i < lines.length; i++) {

            String line = lines[i];

            if (line.isBlank()) {
                continue;
            }

            String[] columns = line.split(",", -1);

            if (columns.length < 9) {
                continue;
            }

            String symbol = columns[0];
            String exchangeSegment = columns[2];
            String symbolName = columns[4];
            String tradingSymbol = columns[5];
            String isin = columns[8];

            scrips.add(
                    new KotakScrip(
                            symbol,
                            exchangeSegment,
                            symbolName,
                            tradingSymbol,
                            isin
                    )
            );
        }

        return scrips;
    }
}
