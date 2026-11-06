package com.dobby.price_alert.service.nse;

import com.dobby.price_alert.dto.nse.derivatives.FiiIndexOiDto;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.Arrays;

@Component
public class FiiIndexOiParser {

    public FiiIndexOiDto parse(String csv, LocalDate date) throws IOException {

        String[] lines = csv.split("\\R");

        int headerLine = -1;

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().startsWith("Client Type")) {
                headerLine = i;
                break;
            }
        }

        if (headerLine == -1) {
            throw new IllegalArgumentException("CSV header row not found");
        }

        String data = String.join("\n",
                Arrays.copyOfRange(lines, headerLine, lines.length));

        try (CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .get()
                .parse(new StringReader(data))) {

            for (CSVRecord record : parser) {

                if (!"FII".equalsIgnoreCase(record.get("Client Type").trim())) {
                    continue;
                }

                long futureIndexLong =
                        Long.parseLong(record.get("Future Index Long"));

                long futureIndexShort =
                        Long.parseLong(record.get("Future Index Short"));

                long optionIndexCallLong =
                        Long.parseLong(record.get("Option Index Call Long"));

                long optionIndexPutLong =
                        Long.parseLong(record.get("Option Index Put Long"));

                long optionIndexCallShort =
                        Long.parseLong(record.get("Option Index Call Short"));

                long optionIndexPutShort =
                        Long.parseLong(record.get("Option Index Put Short"));

                long futuresNet = futureIndexLong - futureIndexShort;

                long optionsNet =
                        (optionIndexCallLong + optionIndexPutLong)
                                - (optionIndexCallShort + optionIndexPutShort);

                long combinedNet = futuresNet + optionsNet;

                long optionsLong = optionIndexCallLong + optionIndexPutLong;
                long optionsShort = optionIndexCallShort + optionIndexPutShort;

                long futuresTotal = futureIndexLong + futureIndexShort;
                long optionsTotal = optionsLong + optionsShort;

                if (futuresTotal == 0 || optionsTotal == 0) {
                    throw new IllegalArgumentException("Invalid OI totals in NSE report");
                }

                double futuresLongPercentage =
                        futureIndexLong * 100.0 / futuresTotal;

                double futuresShortPercentage =
                        futureIndexShort * 100.0 / futuresTotal;

                double optionsLongPercentage =
                        optionsLong * 100.0 / optionsTotal;

                double optionsShortPercentage =
                        optionsShort * 100.0 / optionsTotal;

                return new FiiIndexOiDto(
                        date,
                        futureIndexLong,
                        futureIndexShort,
                        optionIndexCallLong,
                        optionIndexPutLong,
                        optionIndexCallShort,
                        optionIndexPutShort,
                        futuresNet,
                        optionsNet,
                        combinedNet,
                        futuresLongPercentage,
                        futuresShortPercentage,
                        optionsLongPercentage,
                        optionsShortPercentage
                );

            }
        }

        throw new IllegalArgumentException("FII row not found in NSE report");
    }
}