package com.dobby.price_alert.repository;

import com.dobby.price_alert.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {
    Optional<StockAlert> findByWatchlistAndSymbol(
            String watchlist,
            String symbol
    );

    @Query("""
        SELECT s
        FROM StockAlert s
        WHERE s.lastAlertDate = :today
    """)
    List<StockAlert> findAlertsTriggeredToday(@Param("today") LocalDate today);
}
