package com.crewmeister.forex.service;

/**
 * Interface for exchange rate synchronization operations.
 * Handles data synchronization from external APIs.
 */
public interface IExchangeRateSyncService {

    /**
     * Syncs exchange rate data from external API.
     */
    void syncExchangeRates();

    /**
     * Syncs only the latest exchange rates.
     * Used by scheduled job for daily updates.
     * 
     * @return number of records processed
     */
    int syncLatestExchangeRates();
}
