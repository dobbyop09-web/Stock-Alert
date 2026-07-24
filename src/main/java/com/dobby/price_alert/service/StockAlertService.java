package com.dobby.price_alert.service;

import com.dobby.price_alert.dto.AlertStatus;
import com.dobby.price_alert.entity.StockAlert;
import com.dobby.price_alert.repository.StockAlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Objects;

@Service
public class StockAlertService {
    @Autowired
    private StockAlertRepository repository;

    public AlertStatus shouldSendAlert(String watchlist,
                                       String symbol,
                                       double currentPrice,
                                       double alertPrice) {



        StockAlert stockAlert = repository
                .findByWatchlistAndSymbol(watchlist, symbol)
                .orElseGet(() -> createNew(watchlist, symbol));

        boolean triggeredToday =
                LocalDate.now().equals(stockAlert.getLastAlertDate());

        if (currentPrice <= alertPrice && !triggeredToday) {
            stockAlert.setAlertPrice(alertPrice);
            stockAlert.setLastAlertDate(LocalDate.now());
            repository.save(stockAlert);

            return new AlertStatus(true, true);
        }

      return new AlertStatus(
                false,
                triggeredToday
        );
    }

    private StockAlert createNew(String watchlist,
                                 String symbol) {

        StockAlert stockAlert = new StockAlert();

        stockAlert.setWatchlist(watchlist);
        stockAlert.setSymbol(symbol);

        return stockAlert;
    }
}
