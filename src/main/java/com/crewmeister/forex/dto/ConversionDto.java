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
@Schema(description = "Currency conversion result")
public class ConversionDto {

    @Schema(description = "Source currency code", example = "EUR", required = true)
    private String fromCurrency;

    @Schema(description = "Source amount", example = "100.00", required = true)
    private BigDecimal fromAmount;

    @Schema(description = "Target currency code", example = "USD", required = true)
    private String toCurrency;

    @Schema(description = "Converted amount", example = "116.41", required = true)
    private BigDecimal toAmount;

    @Schema(description = "Date of the exchange rate used", example = "2026-03-10", required = true)
    private LocalDate date;

    @Schema(description = "Exchange rate used for conversion", example = "1.1641", required = true)
    private BigDecimal exchangeRate;
}
