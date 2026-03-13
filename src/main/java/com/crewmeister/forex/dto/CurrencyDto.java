package com.crewmeister.forex.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Currency information")
public class CurrencyDto {

    @Schema(description = "ISO 4217 currency code", example = "USD", required = true)
    private String code;

    @Schema(description = "Currency name", example = "United States Dollar", required = true)
    private String name;
}