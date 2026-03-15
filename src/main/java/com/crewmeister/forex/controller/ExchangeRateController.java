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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/forex-rates")
@RequiredArgsConstructor
@Tag(name = "Forex Rates", description = "Foreign exchange rate endpoints for fetching rates, history, and currency conversion")
public class ExchangeRateController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ExchangeRateService exchangeRateService;

    @Operation(
            summary = "Get exchange rates from base currency",
            description = "Retrieves exchange rates from a base currency to all target currencies, grouped by date. " +
                    "Returns latest rates by default, or rates for a specific date."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved exchange rates grouped by date"),
            @ApiResponse(responseCode = "400", description = "Invalid currency code"),
            @ApiResponse(responseCode = "404", description = "Currency not found")
    })
    @GetMapping("/{base}")
    public ResponseEntity<List<ExchangeRatesByDateDto>> getExchangeRatesFromBase(
            @Parameter(description = "Base/source currency code (e.g., EUR, USD)", example = "EUR")
            @PathVariable @Pattern(regexp = "[a-zA-Z]{3}", message = "Currency code must be exactly 3 letters") String base,
            @Parameter(description = "Specific date for exchange rates (YYYY-MM-DD)", example = "2026-03-10")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("GET /api/forex-rates/{} - date={}", base, date);

        List<ExchangeRatesByDateDto> rates = exchangeRateService.getExchangeRatesFromGrouped(base, date);
        return ResponseEntity.ok(rates);
    }

    @Operation(
            summary = "Get exchange rate history from base currency",
            description = "Retrieves all historical exchange rates from a base currency to all target currencies, grouped by date."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved exchange rate history grouped by date"),
            @ApiResponse(responseCode = "400", description = "Invalid currency code"),
            @ApiResponse(responseCode = "404", description = "Currency not found")
    })
    @GetMapping("/{base}/history")
    public ResponseEntity<List<ExchangeRatesByDateDto>> getExchangeRatesFromBaseHistory(
            @Parameter(description = "Base/source currency code (e.g., EUR, USD)", example = "EUR")
            @PathVariable @Pattern(regexp = "[a-zA-Z]{3}", message = "Currency code must be exactly 3 letters") String base) {

        log.info("GET /api/forex-rates/{}/history", base);

        List<ExchangeRatesByDateDto> rates = exchangeRateService.getExchangeRatesFromGroupedHistory(base);
        return ResponseEntity.ok(rates);
    }

    @Operation(
            summary = "Get exchange rate history from base currency with pagination",
            description = "Retrieves paginated historical exchange rates from a base currency to all target currencies, grouped by date. " +
                    "Each page contains grouped rates for N dates (where N = page size)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated exchange rates"),
            @ApiResponse(responseCode = "400", description = "Invalid currency code or pagination parameters"),
            @ApiResponse(responseCode = "404", description = "Currency not found")
    })
    @GetMapping("/{base}/history/paginated")
    public ResponseEntity<Page<ExchangeRatesByDateDto>> getExchangeRatesFromBaseHistoryPaginated(
            @Parameter(description = "Base/source currency code (e.g., EUR, USD)", example = "EUR")
            @PathVariable @Pattern(regexp = "[a-zA-Z]{3}", message = "Currency code must be exactly 3 letters") String base,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) Integer size) {

        log.info("GET /api/forex-rates/{}/history/paginated - page={}, size={}", base, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<ExchangeRatesByDateDto> paginatedRates = exchangeRateService.getExchangeRatesFromGroupedPaginated(base, pageable);
        return ResponseEntity.ok(paginatedRates);
    }

    @Operation(
            summary = "Get exchange rate for currency pair",
            description = "Retrieves the exchange rate for a specific currency pair. " +
                    "Returns the latest rate by default, or the rate on a specific date."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved exchange rate"),
            @ApiResponse(responseCode = "400", description = "Invalid currency code"),
            @ApiResponse(responseCode = "404", description = "Exchange rate not found")
    })
    @GetMapping("/{base}/{target}")
    public ResponseEntity<ExchangeRateDto> getExchangeRate(
            @Parameter(description = "Base/source currency code", example = "EUR")
            @PathVariable @Pattern(regexp = "[a-zA-Z]{3}", message = "Currency code must be exactly 3 letters") String base,
            @Parameter(description = "Target/quote currency code", example = "USD")
            @PathVariable @Pattern(regexp = "[a-zA-Z]{3}", message = "Currency code must be exactly 3 letters") String target,
            @Parameter(description = "Specific date (YYYY-MM-DD)", example = "2026-03-10")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("GET /api/forex-rates/{}/{} - date={}", base, target, date);

        ExchangeRateDto rate = exchangeRateService.getExchangeRate(base, target, date);
        return ResponseEntity.ok(rate);
    }

    @Operation(
            summary = "Get exchange rate history for currency pair",
            description = "Retrieves all historical exchange rates for a specific currency pair."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved exchange rate history"),
            @ApiResponse(responseCode = "400", description = "Invalid currency code"),
            @ApiResponse(responseCode = "404", description = "Exchange rate not found")
    })
    @GetMapping("/{base}/{target}/history")
    public ResponseEntity<List<ExchangeRateDto>> getExchangeRateHistory(
            @Parameter(description = "Base/source currency code", example = "EUR")
            @PathVariable @Pattern(regexp = "[a-zA-Z]{3}", message = "Currency code must be exactly 3 letters") String base,
            @Parameter(description = "Target/quote currency code", example = "USD")
            @PathVariable @Pattern(regexp = "[a-zA-Z]{3}", message = "Currency code must be exactly 3 letters") String target) {

        log.info("GET /api/forex-rates/{}/{}/history", base, target);

        List<ExchangeRateDto> rates = exchangeRateService.getExchangeRateHistory(base, target);
        return ResponseEntity.ok(rates);
    }

    @Operation(
            summary = "Get exchange rate history with pagination",
            description = "Retrieves paginated historical exchange rates for a specific currency pair."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated exchange rate history"),
            @ApiResponse(responseCode = "400", description = "Invalid currency code or pagination parameters"),
            @ApiResponse(responseCode = "404", description = "Exchange rate not found")
    })
    @GetMapping("/{base}/{target}/history/paginated")
    public ResponseEntity<Page<ExchangeRateDto>> getExchangeRateHistoryPaginated(
            @Parameter(description = "Base/source currency code", example = "EUR")
            @PathVariable @Pattern(regexp = "[a-zA-Z]{3}", message = "Currency code must be exactly 3 letters") String base,
            @Parameter(description = "Target/quote currency code", example = "USD")
            @PathVariable @Pattern(regexp = "[a-zA-Z]{3}", message = "Currency code must be exactly 3 letters") String target,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) Integer size) {

        log.info("GET /api/forex-rates/{}/{}/history/paginated - page={}, size={}", base, target, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<ExchangeRateDto> paginatedRates = exchangeRateService.getExchangeRateHistoryPaginated(base, target, pageable);
        return ResponseEntity.ok(paginatedRates);
    }

    @Operation(
            summary = "Convert currency amount",
            description = "Converts an amount from one currency to another. " +
                    "Uses latest exchange rate if no date is provided, or the rate on the specific date."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully converted currency amount"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
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
