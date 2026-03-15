package com.crewmeister.forex.mapper;

import com.crewmeister.forex.dto.CurrencyDto;
import com.crewmeister.forex.entity.Currency;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Currency entities and DTOs.
 * Centralizes all mapping logic for currency objects.
 */
@Component
public class CurrencyMapper {

    /**
     * Converts a Currency entity to CurrencyDto.
     */
    public CurrencyDto toDto(Currency currency) {
        return new CurrencyDto(currency.getCode(), currency.getName());
    }
}
