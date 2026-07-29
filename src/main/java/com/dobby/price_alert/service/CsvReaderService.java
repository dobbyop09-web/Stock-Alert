package com.dobby.price_alert.service;

import com.dobby.price_alert.dto.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Component
public class CsvReaderService {
    private static final Logger log =
            LoggerFactory.getLogger(CsvReaderService.class);

    public List<CsvRecordDto> readCsv(SheetConfig sheetConfig) throws IOException {
        Reader reader = new InputStreamReader(
                new URL(sheetConfig.getUrl()).openStream());

        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(reader);

        List<CsvRecordDto> recordList = new ArrayList<>();
        for (CSVRecord record : records) {
            recordList.add(
                    CsvRecordDto.builder()
                            .symbol(record.get("Symbol").trim())
                            .alertPrice(Double.parseDouble(record.get("Alert Price")))
                            .fib(Double.parseDouble(record.get("FIB")))
                            .rowNumber((int) record.getRecordNumber() + 1)
                            .sheetName(sheetConfig.getName())
                            .build()
            );
        }
     return recordList;
    }
}
