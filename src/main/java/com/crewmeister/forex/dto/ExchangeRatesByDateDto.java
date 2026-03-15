package com.crewmeister.forex.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Schema(description = "Exchange rates grouped by date")
public record ExchangeRatesByDateDto(

        @Schema(description = "Date of the exchange rates", example = "2026-03-10")
        LocalDate date,

        @Schema(description = "Base/source currency code", example = "EUR")
        String baseCurrency,

        @Schema(description = "Map of target currency codes to their exchange rates",
                example = "{\"USD\": 1.1641, \"GBP\": 0.8532}")
        Map<String, BigDecimal> rates
) {}

