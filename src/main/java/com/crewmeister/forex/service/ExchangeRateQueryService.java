package com.crewmeister.forex.service;

import com.crewmeister.forex.dto.ExchangeRateDto;
import com.crewmeister.forex.dto.ExchangeRatesByDateDto;
import com.crewmeister.forex.entity.ExchangeRate;
import com.crewmeister.forex.exception.ResourceNotFoundException;
import com.crewmeister.forex.mapper.ExchangeRateMapper;
import com.crewmeister.forex.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * Get all exchange rates from a base currency grouped by date.
     * Supports: latest, specific date, or all dates.
     */
    @Transactional(readOnly = true)
    public List<ExchangeRatesByDateDto> getExchangeRatesFromGrouped(
            String base, LocalDate date, Boolean allDates) {

        String upperBase = base.toUpperCase();
        List<ExchangeRate> rates;

        if (date != null) {
            rates = exchangeRateRepository.findBySourceCurrencyAndDate(upperBase, date);
        } else if (allDates) {
            rates = exchangeRateRepository.findBySourceCurrencyOrderByDateDesc(upperBase);
        } else {
            rates = exchangeRateRepository.findLatestRatesBySourceCurrency(upperBase);
        }

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
                        upperBase,
                        entry.getValue()
                ))
                .collect(Collectors.toList());
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
    public List<ExchangeRateDto> getExchangeRateHistory(String base, String target, LocalDate date) {
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
