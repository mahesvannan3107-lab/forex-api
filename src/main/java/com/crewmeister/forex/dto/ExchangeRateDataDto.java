package com.crewmeister.forex.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateDataDto {

    private String sourceCurrency;      // EUR (partner currency)
    private String targetCurrency;      // Foreign currency code
    private LocalDate date;             // TIME_PERIOD
    private BigDecimal rate;            // OBS_VALUE
    private String frequency;           // BBK_STD_FREQ (D = Daily)
    private BigDecimal diffPercent;     // BBK_DIFF (percentage change)
}

