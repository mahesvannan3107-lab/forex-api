package com.crewmeister.forex.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Currency conversion result")
public record ConversionDto(

        @Schema(description = "Source currency code", example = "EUR")
        String fromCurrency,

        @Schema(description = "Source amount", example = "100.00")
        BigDecimal fromAmount,

        @Schema(description = "Target currency code", example = "USD")
        String toCurrency,

        @Schema(description = "Converted amount", example = "116.41")
        BigDecimal toAmount,

        @Schema(description = "Date of the exchange rate used", example = "2026-03-10")
        LocalDate date,

        @Schema(description = "Exchange rate used for conversion", example = "1.1641")
        BigDecimal exchangeRate
) {}
