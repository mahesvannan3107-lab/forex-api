package com.crewmeister.forex.service;

import com.crewmeister.forex.dto.ConversionDto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Interface for currency conversion operations.
 */
public interface ICurrencyConversionService {

    /**
     * Convert an amount from one currency to another.
     * Uses latest rate if date is null, or specific date rate if provided.
     */
    ConversionDto convertCurrency(String from, String to, BigDecimal amount, LocalDate date);
}
