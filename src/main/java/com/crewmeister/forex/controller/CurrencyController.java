package com.crewmeister.forex.controller;

import com.crewmeister.forex.dto.CurrencyDto;
import com.crewmeister.forex.service.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/currencies")
@RequiredArgsConstructor
@Tag(name = "Currencies", description = "Currency management endpoints")
public class CurrencyController {

    private final CurrencyService currencyService;

    @Operation(
            summary = "Get all currencies",
            description = "Retrieves a list of all available currencies supported by the system"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved list of currencies",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CurrencyDto.class)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<List<CurrencyDto>> getAllCurrencies() {
        log.info("GET /api/currencies - Fetching all available currencies");
        List<CurrencyDto> currencies = currencyService.getAllCurrencies();
        return ResponseEntity.ok(currencies);
    }
}

