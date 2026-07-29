package com.dobby.price_alert.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {
    @Bean
    public ExecutorService marketDataExecutor() {
        return Executors.newFixedThreadPool(20);
    }
}
