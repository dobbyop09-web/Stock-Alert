package com.dobby.price_alert.repository;

import com.dobby.price_alert.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {
    Optional<StockAlert> findByWatchlistAndSymbol(
            String watchlist,
            String symbol
    );
}
