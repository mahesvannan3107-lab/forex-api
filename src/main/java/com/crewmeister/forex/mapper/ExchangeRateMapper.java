package com.crewmeister.forex.mapper;

import com.crewmeister.forex.dto.ExchangeRateDataDto;
import com.crewmeister.forex.dto.ExchangeRateDto;
import com.crewmeister.forex.entity.ExchangeRate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Mapper for converting between ExchangeRate entities and DTOs.
 * Centralizes all mapping logic for exchange rate objects.
 */
@Component
public class ExchangeRateMapper {

    /**
     * Converts an ExchangeRate entity to ExchangeRateDto.
     */
    public ExchangeRateDto toDto(ExchangeRate exchangeRate) {
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
     * Converts ExchangeRateDataDto (from Bundesbank API) to ExchangeRate entity.
     */
    public ExchangeRate toEntity(ExchangeRateDataDto dto, String createdBy) {
        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setSourceCurrency(dto.sourceCurrency());
        exchangeRate.setTargetCurrency(dto.targetCurrency());
        exchangeRate.setDate(dto.date());
        exchangeRate.setRate(dto.rate());
        exchangeRate.setFrequency(dto.frequency());
        exchangeRate.setDiffPercent(dto.diffPercent());
        exchangeRate.setCreatedBy(createdBy);
        return exchangeRate;
    }

    /**
     * Updates an existing ExchangeRate entity with new data.
     */
    public void updateEntity(ExchangeRate existing, ExchangeRateDataDto dto, String updatedBy) {
        existing.setRate(dto.rate());
        existing.setDiffPercent(dto.diffPercent());
        existing.setUpdatedBy(updatedBy);
    }
}
