package com.crewmeister.forex.service;

import com.crewmeister.forex.client.BundesbankClient;
import com.crewmeister.forex.dto.ExchangeRateDataDto;
import com.crewmeister.forex.dto.ExchangeRatesByDateDto;
import com.crewmeister.forex.entity.ExchangeRate;
import com.crewmeister.forex.dto.ExchangeRateDto;
import com.crewmeister.forex.exception.ResourceNotFoundException;
import com.crewmeister.forex.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final BundesbankClient bundesbankClient;

    /**
     * Get all exchange rates from a base currency grouped by date
     * Supports: latest, specific date, or all dates
     */
    @Transactional(readOnly = true)
    public List<ExchangeRatesByDateDto> getExchangeRatesFromGrouped(
            String base, LocalDate date, Boolean allDates) {

        String upperBase = base.toUpperCase();
        List<ExchangeRate> rates;

        if (date != null) {
            // Specific date - all target currencies
            rates = exchangeRateRepository.findBySourceCurrencyAndDate(upperBase, date);
        } else if (allDates) {
            // All dates for all target currencies
            rates = exchangeRateRepository.findBySourceCurrencyOrderByDateDesc(upperBase);
        } else {
            // Latest rate for each target currency
            rates = exchangeRateRepository.findLatestRatesBySourceCurrency(upperBase);
        }

        // Group by date
        Map<LocalDate, Map<String, BigDecimal>> groupedByDate = rates.stream()
                .collect(Collectors.groupingBy(
                        ExchangeRate::getDate,
                        LinkedHashMap::new,
                        Collectors.toMap(
                                ExchangeRate::getTargetCurrency,
                                ExchangeRate::getRate,
                                (r1, r2) -> r1,
                                LinkedHashMap::new
                        )
                ));

        // Convert to DTO list
        return groupedByDate.entrySet().stream()
                .map(entry -> new ExchangeRatesByDateDto(
                        entry.getKey(),
                        upperBase,
                        entry.getValue()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Get single exchange rate (latest or specific date)
     * If direct pair (base/target) is not found, tries inverse pair (target/base) and calculates inverse rate
     */
    @Transactional(readOnly = true)
    public ExchangeRateDto getExchangeRate(String base, String target, LocalDate date) {
        log.debug("Fetching exchange rate from {} to {} on {}", base, target, date);

        String upperBase = base.toUpperCase();
        String upperTarget = target.toUpperCase();

        ExchangeRate exchangeRate;

        if (date != null) {
            // Specific date - try direct pair first
            exchangeRate = exchangeRateRepository
                    .findBySourceCurrencyAndTargetCurrencyAndDate(upperBase, upperTarget, date)
                    .orElse(null);

            // If not found, try inverse pair
            if (exchangeRate == null) {
                exchangeRate = exchangeRateRepository
                        .findBySourceCurrencyAndTargetCurrencyAndDate(upperTarget, upperBase, date)
                        .map(this::calculateInverseRate)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                String.format("Exchange rate not found for %s to %s on date %s",
                                        upperBase, upperTarget, date)));
            }
        } else {
            // Latest rate - try direct pair first
            exchangeRate = exchangeRateRepository
                    .findLatestRateBySourceAndTarget(upperBase, upperTarget)
                    .orElse(null);

            // If not found, try inverse pair
            if (exchangeRate == null) {
                exchangeRate = exchangeRateRepository
                        .findLatestRateBySourceAndTarget(upperTarget, upperBase)
                        .map(this::calculateInverseRate)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                String.format("Exchange rate not found for %s to %s",
                                        upperBase, upperTarget)));
            }
        }

        return toDto(exchangeRate);
    }

    /**
     * Get exchange rate history for a currency pair
     * If direct pair (base/target) is not found, tries inverse pair (target/base) and calculates inverse rates
     */
    @Transactional(readOnly = true)
    public List<ExchangeRateDto> getExchangeRateHistory(
            String base, String target, LocalDate date) {

        log.debug("Fetching exchange rate history from {} to {}", base, target);

        String upperBase = base.toUpperCase();
        String upperTarget = target.toUpperCase();

        // Try direct pair first
        List<ExchangeRate> rates = exchangeRateRepository
                .findBySourceCurrencyAndTargetCurrencyOrderByDateDesc(upperBase, upperTarget);

        // If no direct pair found, try inverse pair
        if (rates.isEmpty()) {
            rates = exchangeRateRepository
                    .findBySourceCurrencyAndTargetCurrencyOrderByDateDesc(upperTarget, upperBase)
                    .stream()
                    .map(this::calculateInverseRate)
                    .collect(Collectors.toList());
        }

        return rates.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ExchangeRateDto toDto(ExchangeRate exchangeRate) {
        String baseCurrency = exchangeRate.getSourceCurrency();
        String targetCurrency = exchangeRate.getTargetCurrency();
        BigDecimal rate = exchangeRate.getRate();

        String pair = baseCurrency + "/" + targetCurrency;
        String description = String.format("1 %s = %s %s", baseCurrency, rate, targetCurrency);

        return new ExchangeRateDto(
                pair,
                baseCurrency,
                targetCurrency,
                exchangeRate.getDate(),
                rate,
                description
        );
    }

    /**
     * Calculate inverse exchange rate
     * If EUR/INR = 90, then INR/EUR = 1/90
     */
    private ExchangeRate calculateInverseRate(ExchangeRate original) {
        BigDecimal inverseRate = BigDecimal.ONE.divide(original.getRate(), 6, RoundingMode.HALF_UP);

        return new ExchangeRate(
                original.getTargetCurrency(),  // Swap: target becomes source
                original.getSourceCurrency(),  // Swap: source becomes target
                original.getDate(),
                inverseRate
        );
    }

    /**
     * Syncs exchange rate data from Bundesbank API.
     * Fetches all available columns and maps them to ExchangeRate entity.
     */
    @Transactional
    public void syncExchangeRates() {
        log.info("Starting exchange rate synchronization");

        List<ExchangeRateDataDto> allRates = bundesbankClient.fetchAllExchangeRates();

        for (ExchangeRateDataDto dto : allRates) {

            ExchangeRate exchangeRate = new ExchangeRate();
            exchangeRate.setSourceCurrency(dto.getSourceCurrency());
            exchangeRate.setTargetCurrency(dto.getTargetCurrency());
            exchangeRate.setDate(dto.getDate());
            exchangeRate.setRate(dto.getRate());
            exchangeRate.setFrequency(dto.getFrequency());
            exchangeRate.setDiffPercent(dto.getDiffPercent());
            exchangeRate.setCreatedBy("ADMIN");

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
            // Check if exchange rate already exists for this currency pair and date
            ExchangeRate existingRate = exchangeRateRepository
                    .findBySourceCurrencyAndTargetCurrencyAndDate(
                            dto.getSourceCurrency(),
                            dto.getTargetCurrency(),
                            dto.getDate())
                    .orElse(null);

            if (existingRate != null) {
                // Update existing record
                existingRate.setRate(dto.getRate());
                existingRate.setDiffPercent(dto.getDiffPercent());
                existingRate.setUpdatedBy("SCHEDULER");
                exchangeRateRepository.save(existingRate);
                updatedCount++;
            } else {
                // Insert new record
                ExchangeRate newRate = new ExchangeRate();
                newRate.setSourceCurrency(dto.getSourceCurrency());
                newRate.setTargetCurrency(dto.getTargetCurrency());
                newRate.setDate(dto.getDate());
                newRate.setRate(dto.getRate());
                newRate.setFrequency(dto.getFrequency());
                newRate.setDiffPercent(dto.getDiffPercent());
                newRate.setCreatedBy("SCHEDULER");
                exchangeRateRepository.save(newRate);
                insertedCount++;
            }
        }

        log.info("Latest exchange rate synchronization completed - Inserted: {}, Updated: {}", 
                insertedCount, updatedCount);
        return insertedCount + updatedCount;
    }
}
