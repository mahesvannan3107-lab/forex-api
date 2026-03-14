package com.crewmeister.forex.service;

import com.crewmeister.forex.client.BundesbankClient;
import com.crewmeister.forex.dto.ExchangeRateDataDto;
import com.crewmeister.forex.entity.ExchangeRate;
import com.crewmeister.forex.mapper.ExchangeRateMapper;
import com.crewmeister.forex.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for synchronizing exchange rate data from external APIs.
 * Handles all write operations for exchange rates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateSyncService implements IExchangeRateSyncService {

    private static final String SYSTEM_USER_ADMIN = "ADMIN";
    private static final String SYSTEM_USER_SCHEDULER = "SCHEDULER";

    private final ExchangeRateRepository exchangeRateRepository;
    private final BundesbankClient bundesbankClient;
    private final ExchangeRateMapper exchangeRateMapper;

    /**
     * Syncs exchange rate data from Bundesbank API.
     * Fetches all available columns and maps them to ExchangeRate entity.
     */
    @Transactional
    public void syncExchangeRates() {
        log.info("Starting exchange rate synchronization");

        List<ExchangeRateDataDto> allRates = bundesbankClient.fetchAllExchangeRates();

        for (ExchangeRateDataDto dto : allRates) {
            ExchangeRate exchangeRate = exchangeRateMapper.toEntity(dto, SYSTEM_USER_ADMIN);
            exchangeRateRepository.save(exchangeRate);
        }

        log.info("Exchange rate synchronization completed");
    }

    /**
     * Syncs only the latest exchange rates from Bundesbank API.
     * Used by scheduled job for daily updates.
     * Updates existing records or inserts new ones (upsert logic).
     */
    @Transactional
    public int syncLatestExchangeRates() {
        log.info("Starting latest exchange rate synchronization");

        List<ExchangeRateDataDto> latestRates = bundesbankClient.fetchLatestExchangeRates();
        int updatedCount = 0;
        int insertedCount = 0;

        for (ExchangeRateDataDto dto : latestRates) {
            ExchangeRate existingRate = exchangeRateRepository
                    .findBySourceCurrencyAndTargetCurrencyAndDate(
                            dto.getSourceCurrency(),
                            dto.getTargetCurrency(),
                            dto.getDate())
                    .orElse(null);

            if (existingRate != null) {
                exchangeRateMapper.updateEntity(existingRate, dto, SYSTEM_USER_SCHEDULER);
                exchangeRateRepository.save(existingRate);
                updatedCount++;
            } else {
                ExchangeRate newRate = exchangeRateMapper.toEntity(dto, SYSTEM_USER_SCHEDULER);
                exchangeRateRepository.save(newRate);
                insertedCount++;
            }
        }

        log.info("Latest exchange rate synchronization completed - Inserted: {}, Updated: {}", 
                insertedCount, updatedCount);
        return insertedCount + updatedCount;
    }
}
