package com.crewmeister.forex.controller;

import com.crewmeister.forex.dto.ConversionDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
                    "Supports latest rates, specific date, or all dates. Use pagination for large datasets."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved exchange rates grouped by date"),
            @ApiResponse(responseCode = "404", description = "Currency not found")
    })
    @GetMapping("/{base}")
    public ResponseEntity<?> getExchangeRatesFromBase(
            @Parameter(description = "Base/source currency code (e.g., EUR, USD)", example = "EUR")
            @PathVariable String base,
            @Parameter(description = "Specific date for exchange rates (YYYY-MM-DD)", example = "2026-03-10")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Return all dates with exchange rate data", example = "false")
            @RequestParam(required = false, defaultValue = "false") Boolean allDates,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(required = false) Integer size) {

        log.info("GET /api/forex-rates/{} - date={}, allDates={}, page={}, size={}",
                base, date, allDates, page, size);

        // Use pagination if page/size provided and allDates=true
        if (allDates && page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size);
            Page<ExchangeRatesByDateDto> paginatedRates = exchangeRateService.getExchangeRatesFromGroupedPaginated(base, pageable);
            return ResponseEntity.ok(paginatedRates);
        }

        List<ExchangeRatesByDateDto> rates = exchangeRateService.getExchangeRatesFromGrouped(
                base, date, allDates);
        return ResponseEntity.ok(rates);
    }

    @Operation(
            summary = "Get exchange rate for currency pair",
            description = "Retrieves exchange rate(s) for a specific currency pair. " +
                    "Returns single rate by default, or list for all dates. Use pagination for large datasets."
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
            @RequestParam(required = false, defaultValue = "false") Boolean allDates,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(required = false) Integer size) {

        log.info("GET /api/forex-rates/{}/{} - date={}, allDates={}, page={}, size={}",
                base, target, date, allDates, page, size);

        if (allDates) {
            // Use pagination if page/size provided
            if (page != null && size != null) {
                Pageable pageable = PageRequest.of(page, size);
                Page<ExchangeRateDto> paginatedRates = exchangeRateService.getExchangeRateHistoryPaginated(
                        base, target, pageable);
                return ResponseEntity.ok(paginatedRates);
            }
            
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

    @Operation(
            summary = "Convert currency amount",
            description = "Converts an amount from one currency to another. " +
                    "Uses latest exchange rate if no date is provided, or the rate on the specific date."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully converted currency amount"),
            @ApiResponse(responseCode = "404", description = "Exchange rate not found for the given currency pair")
    })
    @GetMapping("/convert")
    public ResponseEntity<ConversionDto> convertCurrency(
            @Parameter(description = "Source currency code", example = "EUR", required = true)
            @RequestParam String from,
            @Parameter(description = "Target currency code", example = "USD", required = true)
            @RequestParam String to,
            @Parameter(description = "Amount to convert", example = "100", required = true)
            @RequestParam java.math.BigDecimal amount,
            @Parameter(description = "Specific date for exchange rate (YYYY-MM-DD). Uses latest if not provided", example = "2026-03-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("GET /api/forex-rates/convert?from={}&to={}&amount={}&date={}",
                from, to, amount, date);

        ConversionDto conversion = exchangeRateService.convertCurrency(from, to, amount, date);
        return ResponseEntity.ok(conversion);
    }

}
