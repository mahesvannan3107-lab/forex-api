package com.crewmeister.forex.service;

import com.crewmeister.forex.dto.ConversionDto;
import com.crewmeister.forex.dto.ExchangeRateDto;
import com.crewmeister.forex.dto.ExchangeRatesByDateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Facade service for exchange rate operations.
 * Delegates to specialized services for specific responsibilities.
 * Maintained for backward compatibility with existing controllers and tests.
 * 
 * @see ExchangeRateQueryService for read operations
 * @see CurrencyConversionService for conversion operations
 * @see ExchangeRateSyncService for sync operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final IExchangeRateQueryService exchangeRateQueryService;
    private final ICurrencyConversionService currencyConversionService;
    private final IExchangeRateSyncService exchangeRateSyncService;

    /**
     * Get exchange rates from a base currency grouped by date.
     * Delegates to ExchangeRateQueryService.
     */
    public List<ExchangeRatesByDateDto> getExchangeRatesFromGrouped(String base, LocalDate date) {
        return exchangeRateQueryService.getExchangeRatesFromGrouped(base, date);
    }

    /**
     * Get all historical exchange rates from a base currency grouped by date.
     * Delegates to ExchangeRateQueryService.
     */
    public List<ExchangeRatesByDateDto> getExchangeRatesFromGroupedHistory(String base) {
        return exchangeRateQueryService.getExchangeRatesFromGroupedHistory(base);
    }

    /**
     * Get single exchange rate (latest or specific date).
     * Delegates to ExchangeRateQueryService.
     */
    public ExchangeRateDto getExchangeRate(String base, String target, LocalDate date) {
        return exchangeRateQueryService.getExchangeRate(base, target, date);
    }

    /**
     * Convert an amount from one currency to another.
     * Delegates to CurrencyConversionService.
     */
    public ConversionDto convertCurrency(String from, String to, BigDecimal amount, LocalDate date) {
        return currencyConversionService.convertCurrency(from, to, amount, date);
    }

    /**
     * Get exchange rate history for a currency pair.
     * Delegates to ExchangeRateQueryService.
     */
    public List<ExchangeRateDto> getExchangeRateHistory(String base, String target) {
        return exchangeRateQueryService.getExchangeRateHistory(base, target);
    }

    /**
     * Syncs exchange rate data from Bundesbank API.
     * Delegates to ExchangeRateSyncService.
     */
    public void syncExchangeRates() {
        exchangeRateSyncService.syncExchangeRates();
    }

    /**
     * Syncs only the latest exchange rates from Bundesbank API.
     * Delegates to ExchangeRateSyncService.
     */
    public int syncLatestExchangeRates() {
        return exchangeRateSyncService.syncLatestExchangeRates();
    }

    /**
     * Get all exchange rates from a base currency with pagination by date.
     * Delegates to ExchangeRateQueryService.
     */
    public Page<ExchangeRatesByDateDto> getExchangeRatesFromGroupedPaginated(String base, Pageable pageable) {
        return exchangeRateQueryService.getExchangeRatesFromGroupedPaginated(base, pageable);
    }

    /**
     * Get exchange rate history for a currency pair with pagination.
     * Delegates to ExchangeRateQueryService.
     */
    public Page<ExchangeRateDto> getExchangeRateHistoryPaginated(String base, String target, Pageable pageable) {
        return exchangeRateQueryService.getExchangeRateHistoryPaginated(base, target, pageable);
    }
}
