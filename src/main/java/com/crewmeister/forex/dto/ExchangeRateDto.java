package com.crewmeister.forex.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Exchange rate information for a currency pair")
public class ExchangeRateDto {

    @Schema(description = "Currency pair notation", example = "EUR/USD", required = true)
    private String pair;

    @Schema(description = "Base/source currency code", example = "EUR", required = true)
    private String baseCurrency;

    @Schema(description = "Target/quote currency code", example = "USD", required = true)
    private String targetCurrency;

    @Schema(description = "Date of the exchange rate", example = "2026-03-10", required = true)
    private LocalDate date;

    @Schema(description = "Exchange rate value", example = "1.1641", required = true)
    private BigDecimal rate;

    @Schema(description = "Human-readable description of the rate", example = "1 EUR = 1.1641 USD")
    private String description;
}

