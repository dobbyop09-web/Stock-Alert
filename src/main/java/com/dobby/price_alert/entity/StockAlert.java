package com.dobby.price_alert.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "stock_alert",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"watchlist", "symbol"})
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String watchlist;

    @Column(nullable = false)
    private String symbol;

    private Double alertPrice;

    private LocalDate lastAlertDate;

}
