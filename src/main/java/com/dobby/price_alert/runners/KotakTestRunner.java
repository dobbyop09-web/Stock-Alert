package com.dobby.price_alert.runners;

import com.dobby.price_alert.client.KotakSession;
import com.dobby.price_alert.constants.SheetType;
import com.dobby.price_alert.dto.KotakScrip;
import com.dobby.price_alert.dto.kotak.KotakTotpLoginResponse;
import com.dobby.price_alert.service.*;
import com.dobby.price_alert.service.kotak.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class KotakTestRunner implements CommandLineRunner {

    private final KotakAuthService kotakAuthService;
    private final KotakTotpService kotakTotpService;
    private final KotakScripMasterApiService kotakScripMasterApiService;
    private final KotakScripMasterParser scripMasterParser;
    private final KotakQuoteService kotakQuoteService;
    private final KotakScripLookupService kotakScripLookupService;
    private final CsvReaderService csvReaderService;

    public KotakTestRunner(
            KotakAuthService kotakAuthService,
            KotakTotpService kotakTotpService, KotakScripMasterApiService kotakScripMasterApiService, KotakScripMasterParser scripMasterParser, KotakQuoteService kotakQuoteService, KotakScripLookupService kotakScripLookupService, CsvReaderService csvReaderService) {

        this.kotakAuthService = kotakAuthService;
        this.kotakTotpService = kotakTotpService;
        this.kotakScripMasterApiService = kotakScripMasterApiService;
        this.scripMasterParser = scripMasterParser;
        this.kotakQuoteService = kotakQuoteService;
        this.kotakScripLookupService = kotakScripLookupService;
        this.csvReaderService = csvReaderService;
    }

    @Override
    public void run(String... args) throws IOException {

//        String totp = kotakTotpService.generateTotp();
//
//        System.out.println("Generated TOTP successfully.");
//
//        KotakTotpLoginResponse response  =
//                kotakAuthService.loginWithTotp(totp);
//
//        if (response != null) {
//            System.out.println("Kotak TOTP login successful.");
//            System.out.println("Session data received.");
//        }
//        KotakSession session =
//                kotakAuthService.validateMpin(response);
//
//        if (session != null) {
//            System.out.println("Kotak MPIN validation successful.");
//            System.out.println("Trade session received.");
//        }

//
//        ClassPathResource resource =
//                new ClassPathResource("nse_cm-v1.csv");
//
//        String nseMaster;
//
//        try (InputStream inputStream = resource.getInputStream()) {
//
//            nseMaster = new String(
//                    inputStream.readAllBytes(),
//                    StandardCharsets.UTF_8
//            );
//        }
//        System.out.println(kotakScripMasterApiService.getMasterScripFilePaths(session));
//
//
//
//        System.out.println("NSE master loaded from resources.");
//
////        List<KotakScrip> scrips =
////                scripMasterParser.parse(nseMaster);
////        for (KotakScrip scrip : scrips) {
////
////            if ("BSE".equals(scrip.getSymbolName())) {
////
////                System.out.println("TCS found:");
////                System.out.println(
////                        "Symbol: " + scrip.getSymbol()
////                );
////                System.out.println(
////                        "Exchange Segment: "
////                                + scrip.getExchangeSegment()
////                );
////                System.out.println(
////                        "Trading Symbol: "
////                                + scrip.getTradingSymbol()
////                );
////                System.out.println(
////                        "ISIN: " + scrip.getIsin()
////                );
////
////                break;
////            }
////        }
//        Map<String, KotakScrip> scripMap =
//                kotakScripLookupService.loadNseScripMap();
//
//
////        for (SheetType sheet : SheetType.values()) {
////
////            csvReaderService.testKotakForCsvStocks(sheet.getSheetConfig(),scripMap);
////
////        }
//       List<String> tokens =  List.of("544937");
//           kotakQuoteService.getOhlc("bse_cm",tokens);


    }
}