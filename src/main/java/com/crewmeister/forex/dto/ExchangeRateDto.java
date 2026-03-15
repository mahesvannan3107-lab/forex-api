package com.crewmeister.forex.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Exchange rate information for a currency pair")
public record ExchangeRateDto(

        @Schema(description = "Currency pair notation", example = "EUR/USD")
        String pair,

        @Schema(description = "Base/source currency code", example = "EUR")
        String baseCurrency,

        @Schema(description = "Target/quote currency code", example = "USD")
        String targetCurrency,

        @Schema(description = "Date of the exchange rate", example = "2026-03-10")
        LocalDate date,

        @Schema(description = "Exchange rate value", example = "1.1641")
        BigDecimal rate,

        @Schema(description = "Human-readable description of the rate", example = "1 EUR = 1.1641 USD")
        String description
) {}

