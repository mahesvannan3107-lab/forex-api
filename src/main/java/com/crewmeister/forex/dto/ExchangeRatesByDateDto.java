package com.crewmeister.forex.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Exchange rates grouped by date")
public class ExchangeRatesByDateDto {

    @Schema(description = "Date of the exchange rates", example = "2026-03-10", required = true)
    private LocalDate date;

    @Schema(description = "Base/source currency code", example = "EUR", required = true)
    private String baseCurrency;

    @Schema(description = "Map of target currency codes to their exchange rates",
            example = "{\"USD\": 1.1641, \"GBP\": 0.8532}",
            required = true)
    private Map<String, BigDecimal> rates;
}

