package com.crewmeister.forex.config;

import com.crewmeister.forex.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private final ExchangeRateService exchangeRateService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting data synchronization from Bundesbank API...");

        try {
            exchangeRateService.syncExchangeRates();
            log.info("Data synchronization completed successfully");
        } catch (Exception e) {
            log.error("Failed to synchronize exchange rates: {}", e.getMessage(), e);
        }
    }
}
