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

import java.util.ArrayList;
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
     * Uses batch insert for better performance.
     */
    @Transactional
    @Override
    public void syncExchangeRates() {
        log.info("Starting exchange rate synchronization");

        List<ExchangeRateDataDto> allRates = bundesbankClient.fetchAllExchangeRates();

        List<ExchangeRate> exchangeRates = allRates.stream()
                .map(dto -> exchangeRateMapper.toEntity(dto, SYSTEM_USER_ADMIN))
                .toList();

        exchangeRateRepository.saveAll(exchangeRates);

        log.info("Exchange rate synchronization completed - {} records saved", exchangeRates.size());
    }

    /**
     * Syncs only the latest exchange rates from Bundesbank API.
     * Used by scheduled job for daily updates.
     * Updates existing records or inserts new ones (upsert logic).
     * Uses batch save for better performance.
     */
    @Transactional
    @Override
    public int syncLatestExchangeRates() {
        log.info("Starting latest exchange rate synchronization");

        List<ExchangeRateDataDto> latestRates = bundesbankClient.fetchLatestExchangeRates();
        List<ExchangeRate> toSave = new ArrayList<>();
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
                toSave.add(existingRate);
                updatedCount++;
            } else {
                ExchangeRate newRate = exchangeRateMapper.toEntity(dto, SYSTEM_USER_SCHEDULER);
                toSave.add(newRate);
                insertedCount++;
            }
        }

        exchangeRateRepository.saveAll(toSave);

        log.info("Latest exchange rate synchronization completed - Inserted: {}, Updated: {}", 
                insertedCount, updatedCount);
        return insertedCount + updatedCount;
    }
}
