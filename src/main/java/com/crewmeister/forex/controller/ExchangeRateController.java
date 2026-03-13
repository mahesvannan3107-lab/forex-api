package com.crewmeister.forex.controller;

import com.crewmeister.forex.dto.ExchangeRateDto;
import com.crewmeister.forex.dto.ExchangeRatesByDateDto;
import com.crewmeister.forex.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/forex-rates")
@RequiredArgsConstructor
@Tag(name = "Forex Rates", description = "Foreign exchange rate endpoints for fetching rates, history, and currency conversion")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @Operation(
            summary = "Get exchange rates from base currency",
            description = "Retrieves exchange rates from a base currency to all target currencies, grouped by date. " +
                    "Supports latest rates, specific date, or all dates."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved exchange rates grouped by date"),
            @ApiResponse(responseCode = "404", description = "Currency not found")
    })
    @GetMapping("/{base}")
    public ResponseEntity<List<ExchangeRatesByDateDto>> getExchangeRatesFromBase(
            @Parameter(description = "Base/source currency code (e.g., EUR, USD)", example = "EUR")
            @PathVariable String base,
            @Parameter(description = "Specific date for exchange rates (YYYY-MM-DD)", example = "2026-03-10")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Return all dates with exchange rate data", example = "false")
            @RequestParam(required = false, defaultValue = "false") Boolean allDates) {

        log.info("GET /api/forex-rates/{} - date={}, allDates={}",
                base, date, allDates);

        List<ExchangeRatesByDateDto> rates = exchangeRateService.getExchangeRatesFromGrouped(
                base, date, allDates);
        return ResponseEntity.ok(rates);
    }

    @Operation(
            summary = "Get exchange rate for currency pair",
            description = "Retrieves exchange rate(s) for a specific currency pair. " +
                    "Returns single rate by default, or list for all dates."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved exchange rate(s)"),
            @ApiResponse(responseCode = "404", description = "Exchange rate not found")
    })
    @GetMapping("/{base}/{target}")
    public ResponseEntity<?> getExchangeRate(
            @Parameter(description = "Base/source currency code", example = "EUR")
            @PathVariable String base,
            @Parameter(description = "Target/quote currency code", example = "USD")
            @PathVariable String target,
            @Parameter(description = "Specific date (YYYY-MM-DD)", example = "2026-03-10")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Return all dates with exchange rate data", example = "false")
            @RequestParam(required = false, defaultValue = "false") Boolean allDates) {

        log.info("GET /api/forex-rates/{}/{} - date={}, allDates={}",
                base, target, date, allDates);

        if (allDates) {
            // Return list for all dates
            List<ExchangeRateDto> rates = exchangeRateService.getExchangeRateHistory(
                    base, target, date);
            return ResponseEntity.ok(rates);
        } else {
            // Return single rate for specific date or latest
            ExchangeRateDto rate = exchangeRateService.getExchangeRate(base, target, date);
            return ResponseEntity.ok(rate);
        }
    }

}
