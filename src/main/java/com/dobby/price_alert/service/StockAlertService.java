package com.dobby.price_alert.service;

import com.dobby.price_alert.dto.AlertStatus;
import com.dobby.price_alert.entity.StockAlert;
import com.dobby.price_alert.repository.StockAlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StockAlertService {
    @Autowired
    private StockAlertRepository repository;

    public AlertStatus shouldSendAlert(String watchlist,
                                       String symbol,
                                       double currentPrice,
                                       double alertPrice,
                                       Set<String> triggeredAlerts) {



        if(triggeredAlerts.contains(symbol)){
            return new AlertStatus(false,true);
        }
        if(currentPrice > alertPrice){
            return new AlertStatus(false,false);
        }

        StockAlert stockAlert = repository
                .findByWatchlistAndSymbol(watchlist, symbol)
                .orElseGet(() -> createNew(watchlist, symbol));

            stockAlert.setAlertPrice(alertPrice);
            stockAlert.setLastAlertDate(LocalDate.now());
            repository.save(stockAlert);

            return new AlertStatus(true, true);

    }

    private StockAlert createNew(String watchlist,
                                 String symbol) {

        StockAlert stockAlert = new StockAlert();

        stockAlert.setWatchlist(watchlist);
        stockAlert.setSymbol(symbol);

        return stockAlert;
    }

    public Set<String> getAllTriggeredToday() {
        return repository.findAlertsTriggeredToday(LocalDate.now())
                .stream()
                .map(StockAlert::getSymbol)
                .collect(Collectors.toSet());
    }
}
