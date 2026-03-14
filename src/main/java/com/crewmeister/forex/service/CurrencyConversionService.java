package com.crewmeister.forex.service;

import com.crewmeister.forex.dto.ConversionDto;
import com.crewmeister.forex.dto.ExchangeRateDto;
import com.crewmeister.forex.exception.InvalidParameterException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Service responsible for currency conversion operations.
 * Handles conversion logic and validation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyConversionService implements ICurrencyConversionService {

    private final IExchangeRateQueryService exchangeRateQueryService;

    /**
     * Convert an amount from one currency to another.
     * Uses latest rate if date is null, or specific date rate if provided.
     */
    @Transactional(readOnly = true)
    public ConversionDto convertCurrency(String from, String to, BigDecimal amount, LocalDate date) {
        log.debug("Converting {} {} to {} on {}", amount, from, to, date);

        validateConversionParams(from, to, amount, date);

        ExchangeRateDto rateDto = exchangeRateQueryService.getExchangeRate(from, to, date);
        BigDecimal convertedAmount = amount.multiply(rateDto.getRate()).setScale(2, RoundingMode.HALF_UP);

        return new ConversionDto(
                rateDto.getBaseCurrency(),
                amount,
                rateDto.getTargetCurrency(),
                convertedAmount,
                rateDto.getDate(),
                rateDto.getRate()
        );
    }

    /**
     * Validates conversion request parameters.
     */
    private void validateConversionParams(String from, String to, BigDecimal amount, LocalDate date) {
        if (from == null || from.isBlank()) {
            throw new InvalidParameterException("'from' currency is required");
        }
        if (to == null || to.isBlank()) {
            throw new InvalidParameterException("'to' currency is required");
        }
        if (!from.matches("[a-zA-Z]{3}")) {
            throw new InvalidParameterException("'from' must be a valid 3-letter currency code");
        }
        if (!to.matches("[a-zA-Z]{3}")) {
            throw new InvalidParameterException("'to' must be a valid 3-letter currency code");
        }
        if (from.equalsIgnoreCase(to)) {
            throw new InvalidParameterException("'from' and 'to' currencies must be different");
        }
        if (amount == null) {
            throw new InvalidParameterException("'amount' is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidParameterException("'amount' must be greater than zero");
        }
        if (date != null && date.isAfter(LocalDate.now())) {
            throw new InvalidParameterException("'date' cannot be a future date");
        }
    }
}
