package com.crewmeister.forex.scheduler;

import com.crewmeister.forex.service.IExchangeRateSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to fetch and update exchange rates daily from Bundesbank API.
 * Runs every day at 6:00 AM to get the latest exchange rates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledExchangeRateUpdater {

    private final IExchangeRateSyncService exchangeRateSyncService;

    /**
     * Scheduled task to sync latest exchange rates daily.
     * Cron expression is configured in application.properties (scheduler.exchange-rate.cron)
     * Default: "0 0 6 * * ?" - Every day at 6:00 AM
     */
    @Scheduled(cron = "${scheduler.exchange-rate.cron}")
    public void updateExchangeRates() {
        log.info("=== Starting scheduled exchange rate update ===");
        
        try {
            int recordsProcessed = exchangeRateSyncService.syncLatestExchangeRates();
            log.info("=== Scheduled exchange rate update completed successfully. Records processed: {} ===", 
                    recordsProcessed);
        } catch (Exception e) {
            log.error("=== Error during scheduled exchange rate update: {} ===", e.getMessage(), e);
        }
    }

}
