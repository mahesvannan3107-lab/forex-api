package com.crewmeister.forex.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateDataDto(
        String sourceCurrency,      // EUR (partner currency)
        String targetCurrency,      // Foreign currency code
        LocalDate date,             // TIME_PERIOD
        BigDecimal rate,            // OBS_VALUE
        String frequency,           // BBK_STD_FREQ (D = Daily)
        BigDecimal diffPercent      // BBK_DIFF (percentage change)
) {}

