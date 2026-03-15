package com.crewmeister.forex.service;

import com.crewmeister.forex.dto.CurrencyDto;

import java.util.List;

/**
 * Interface for currency query operations.
 * Provides read-only access to currency data.
 */
public interface ICurrencyService {

    /**
     * Get all available currencies.
     */
    List<CurrencyDto> getAllCurrencies();
}
