package com.dobby.price_alert.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class TestEntity {
    @Id
    public String id;
}
