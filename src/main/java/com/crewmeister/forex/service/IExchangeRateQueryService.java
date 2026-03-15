package com.crewmeister.forex.service;

import com.crewmeister.forex.dto.ExchangeRateDto;
import com.crewmeister.forex.dto.ExchangeRatesByDateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface for exchange rate query operations.
 * Provides read-only access to exchange rate data.
 */
public interface IExchangeRateQueryService {

    /**
     * Get exchange rates from a base currency grouped by date.
     * Returns latest rates if date is null, or rates for a specific date.
     */
    List<ExchangeRatesByDateDto> getExchangeRatesFromGrouped(String base, LocalDate date);

    /**
     * Get all historical exchange rates from a base currency grouped by date.
     */
    List<ExchangeRatesByDateDto> getExchangeRatesFromGroupedHistory(String base);

    /**
     * Get all exchange rates from a base currency with pagination by date.
     * Each page contains grouped rates for N dates (where N = page size).
     */
    Page<ExchangeRatesByDateDto> getExchangeRatesFromGroupedPaginated(String base, Pageable pageable);

    /**
     * Get single exchange rate (latest or specific date).
     */
    ExchangeRateDto getExchangeRate(String base, String target, LocalDate date);

    /**
     * Get exchange rate history for a currency pair.
     */
    List<ExchangeRateDto> getExchangeRateHistory(String base, String target);

    /**
     * Get exchange rate history for a currency pair with pagination.
     */
    Page<ExchangeRateDto> getExchangeRateHistoryPaginated(String base, String target, Pageable pageable);
}
