package com.crewmeister.forex.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Currency information")
public record CurrencyDto(
        @Schema(description = "ISO 4217 currency code", example = "USD", required = true)
        String code,

        @Schema(description = "Currency name", example = "United States Dollar", required = true)
        String name
) {
}