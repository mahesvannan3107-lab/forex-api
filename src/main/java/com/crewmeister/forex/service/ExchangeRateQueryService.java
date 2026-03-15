package com.crewmeister.forex.service;

import com.crewmeister.forex.dto.ExchangeRateDto;
import com.crewmeister.forex.dto.ExchangeRatesByDateDto;
import com.crewmeister.forex.entity.ExchangeRate;
import com.crewmeister.forex.exception.ResourceNotFoundException;
import com.crewmeister.forex.mapper.ExchangeRateMapper;
import com.crewmeister.forex.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service responsible for querying exchange rate data.
 * Handles all read operations for exchange rates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateQueryService implements IExchangeRateQueryService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateMapper exchangeRateMapper;

    /**
     * Get exchange rates from a base currency grouped by date.
     * Returns latest rates if date is null, or rates for a specific date.
     */
    @Transactional(readOnly = true)
    @Override
    public List<ExchangeRatesByDateDto> getExchangeRatesFromGrouped(String base, LocalDate date) {

        String upperBase = base.toUpperCase();
        List<ExchangeRate> rates;

        if (date != null) {
            rates = exchangeRateRepository.findBySourceCurrencyAndDate(upperBase, date);
        } else {
            rates = exchangeRateRepository.findLatestRatesBySourceCurrency(upperBase);
        }

        return groupRatesByDate(rates, upperBase);
    }

    /**
     * Get all historical exchange rates from a base currency grouped by date.
     */
    @Transactional(readOnly = true)
    @Override
    public List<ExchangeRatesByDateDto> getExchangeRatesFromGroupedHistory(String base) {

        String upperBase = base.toUpperCase();
        List<ExchangeRate> rates = exchangeRateRepository.findBySourceCurrencyOrderByDateDesc(upperBase);

        return groupRatesByDate(rates, upperBase);
    }

    /**
     * Get single exchange rate (latest or specific date).
     * If direct pair (base/target) is not found, tries inverse pair (target/base) and calculates inverse rate.
     */
    @Transactional(readOnly = true)
    public ExchangeRateDto getExchangeRate(String base, String target, LocalDate date) {
        log.debug("Fetching exchange rate from {} to {} on {}", base, target, date);

        String upperBase = base.toUpperCase();
        String upperTarget = target.toUpperCase();

        ExchangeRate exchangeRate;

        if (date != null) {
            exchangeRate = exchangeRateRepository
                    .findBySourceCurrencyAndTargetCurrencyAndDate(upperBase, upperTarget, date)
                    .orElse(null);

            if (exchangeRate == null) {
                exchangeRate = exchangeRateRepository
                        .findBySourceCurrencyAndTargetCurrencyAndDate(upperTarget, upperBase, date)
                        .map(this::calculateInverseRate)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                String.format("Exchange rate not found for %s to %s on date %s",
                                        upperBase, upperTarget, date)));
            }
        } else {
            exchangeRate = exchangeRateRepository
                    .findLatestRateBySourceAndTarget(upperBase, upperTarget)
                    .orElse(null);

            if (exchangeRate == null) {
                exchangeRate = exchangeRateRepository
                        .findLatestRateBySourceAndTarget(upperTarget, upperBase)
                        .map(this::calculateInverseRate)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                String.format("Exchange rate not found for %s to %s",
                                        upperBase, upperTarget)));
            }
        }

        return exchangeRateMapper.toDto(exchangeRate);
    }

    /**
     * Get exchange rate history for a currency pair.
     * If direct pair (base/target) is not found, tries inverse pair (target/base) and calculates inverse rates.
     */
    @Transactional(readOnly = true)
    @Override
    public List<ExchangeRateDto> getExchangeRateHistory(String base, String target) {
        log.debug("Fetching exchange rate history from {} to {}", base, target);

        String upperBase = base.toUpperCase();
        String upperTarget = target.toUpperCase();

        List<ExchangeRate> rates = exchangeRateRepository
                .findBySourceCurrencyAndTargetCurrencyOrderByDateDesc(upperBase, upperTarget);

        if (rates.isEmpty()) {
            rates = exchangeRateRepository
                    .findBySourceCurrencyAndTargetCurrencyOrderByDateDesc(upperTarget, upperBase)
                    .stream()
                    .map(this::calculateInverseRate)
                    .collect(Collectors.toList());
        }

        return rates.stream()
                .map(exchangeRateMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all exchange rates from a base currency with pagination by date.
     * Each page contains grouped rates for N dates (where N = page size).
     */
    @Transactional(readOnly = true)
    @Override
    public Page<ExchangeRatesByDateDto> getExchangeRatesFromGroupedPaginated(String base, Pageable pageable) {
        log.debug("Fetching paginated exchange rates from {} - page: {}, size: {}", 
                base, pageable.getPageNumber(), pageable.getPageSize());

        String upperBase = base.toUpperCase();
        
        // Get paginated distinct dates
        Page<LocalDate> datesPage = exchangeRateRepository.findDistinctDatesBySourceCurrency(upperBase, pageable);
        
        if (datesPage.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // Fetch all rates for these dates
        List<LocalDate> dates = datesPage.getContent();
        List<ExchangeRate> rates = exchangeRateRepository.findBySourceCurrencyAndDateIn(upperBase, dates);
        
        // Group by date and convert to DTOs
        List<ExchangeRatesByDateDto> content = groupRatesByDate(rates, upperBase);
        
        return new PageImpl<>(content, pageable, datesPage.getTotalElements());
    }

    /**
     * Get exchange rate history for a currency pair with pagination.
     */
    @Transactional(readOnly = true)
    @Override
    public Page<ExchangeRateDto> getExchangeRateHistoryPaginated(String base, String target, Pageable pageable) {
        log.debug("Fetching paginated exchange rate history from {} to {} - page: {}, size: {}", 
                base, target, pageable.getPageNumber(), pageable.getPageSize());

        String upperBase = base.toUpperCase();
        String upperTarget = target.toUpperCase();

        Page<ExchangeRate> ratesPage = exchangeRateRepository
                .findBySourceCurrencyAndTargetCurrencyOrderByDateDesc(upperBase, upperTarget, pageable);

        if (ratesPage.isEmpty()) {
            // Try inverse pair
            Page<ExchangeRate> inverseRatesPage = exchangeRateRepository
                    .findBySourceCurrencyAndTargetCurrencyOrderByDateDesc(upperTarget, upperBase, pageable);
            
            return inverseRatesPage.map(this::calculateInverseRate)
                    .map(exchangeRateMapper::toDto);
        }

        return ratesPage.map(exchangeRateMapper::toDto);
    }

    /**
     * Groups exchange rates by date into ExchangeRatesByDateDto objects.
     */
    private List<ExchangeRatesByDateDto> groupRatesByDate(List<ExchangeRate> rates, String baseCurrency) {
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

        return groupedByDate.entrySet().stream()
                .map(entry -> new ExchangeRatesByDateDto(
                        entry.getKey(),
                        baseCurrency,
                        entry.getValue()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Calculate inverse exchange rate.
     * If EUR/INR = 90, then INR/EUR = 1/90
     */
    private ExchangeRate calculateInverseRate(ExchangeRate original) {
        BigDecimal inverseRate = BigDecimal.ONE.divide(original.getRate(), 6, RoundingMode.HALF_UP);

        return new ExchangeRate(
                original.getTargetCurrency(),
                original.getSourceCurrency(),
                original.getDate(),
                inverseRate
        );
    }
}
