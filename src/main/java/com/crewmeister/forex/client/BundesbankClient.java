package com.crewmeister.forex.client;

import com.crewmeister.forex.config.BundesbankApiConfig;
import com.crewmeister.forex.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.StringReader;
import com.crewmeister.forex.dto.ExchangeRateDataDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class BundesbankClient {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // CSV column indices for Bundesbank SDMX CSV format
    private static final int COL_FREQUENCY = 1;
    private static final int COL_TARGET_CURRENCY = 2;
    private static final int COL_SOURCE_CURRENCY = 3;
    private static final int COL_TIME_PERIOD = 7;
    private static final int COL_OBS_VALUE = 8;
    private static final int COL_BBK_DIFF = 18;
    private static final int MIN_COLUMNS = 9;

    private final BundesbankApiConfig apiConfig;
    private final RestTemplate restTemplate;

    /**
     * Fetches exchange rates for all currencies from Bundesbank API in a single call.
     * Uses wildcard pattern to get all available currencies.
     * Returns a list of ExchangeRateDataDto with all CSV columns.
     * 
     * Retries up to 3 times with exponential backoff (1s, 2s, 4s) on transient failures.
     */
    @Retryable(
            retryFor = { RestClientException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<ExchangeRateDataDto> fetchAllExchangeRates() {
        try {
            // Wildcard API call: D..EUR.BB.AC.000 fetches all currencies
            String url = buildApiUrl(apiConfig.getDefaultObservations());

            log.info("Fetching all exchange rates from: {}", url);
            String csvData = restTemplate.getForObject(url, String.class);

            if (csvData == null || csvData.isEmpty()) {
                log.warn("No data received from Bundesbank API");
                return Collections.emptyList();
            }

            List<ExchangeRateDataDto> result = parseAllCurrenciesData(csvData);
            log.info("Successfully fetched {} exchange rate records", result.size());
            return result;

        } catch (Exception e) {
            throw new ExternalApiException("Failed to fetch exchange rates from Bundesbank API", e);
        }
    }

    /**
     * Parses Bundesbank CSV data containing multiple currencies.
     * CSV format: DATAFLOW,FREQ,CURRENCY,PARTNER_CURRENCY,...,TIME_PERIOD,OBS_VALUE,...,BBK_DIFF,OBS_STATUS
     * Extracts all relevant columns and maps them to ExchangeRateDataDto.
     */
    private List<ExchangeRateDataDto> parseAllCurrenciesData(String csvData) {
        List<ExchangeRateDataDto> result = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(csvData))) {
            String line;
            boolean headerSkipped = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines, comments, and quoted lines
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("\"")) {
                    continue;
                }

                // Skip header
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                // Split by comma but respect quoted fields
                String[] parts = splitCsvLine(line);
                if (parts.length >= MIN_COLUMNS) {
                    try {
                        String frequency = parts[COL_FREQUENCY].trim();
                        String targetCurrency = parts[COL_TARGET_CURRENCY].trim();
                        String sourceCurrency = parts[COL_SOURCE_CURRENCY].trim();
                        String dateStr = parts[COL_TIME_PERIOD].trim();
                        String valueStr = parts[COL_OBS_VALUE].trim();
                        String diffStr = parts.length > COL_BBK_DIFF ? parts[COL_BBK_DIFF].trim() : "";

                        // Skip rows with no observation (weekends/holidays marked with ".")
                        if (valueStr.isEmpty() || ".".equals(valueStr)) {
                            continue;
                        }

                        LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
                        BigDecimal rate = new BigDecimal(valueStr);
                        BigDecimal diffPercent = null;

                        if (!diffStr.isEmpty() && !".".equals(diffStr)) {
                            try {
                                diffPercent = new BigDecimal(diffStr);
                            } catch (NumberFormatException e) {
                                log.trace("Could not parse diff percent: {}", diffStr);
                            }
                        }

                        ExchangeRateDataDto dto = new ExchangeRateDataDto(
                                sourceCurrency,
                                targetCurrency,
                                date,
                                rate,
                                frequency,
                                diffPercent
                        );

                        result.add(dto);

                    } catch (Exception e) {
                        log.trace("Skipping unparseable line: {}", line);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error parsing CSV data", e);
        }

        return result;
    }

    /**
     * Fetches only the latest exchange rates for all currencies (1 observation per currency).
     * Used for daily scheduled updates.
     * 
     * Retries up to 3 times with exponential backoff (1s, 2s, 4s) on transient failures.
     */
    @Retryable(
            retryFor = { RestClientException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<ExchangeRateDataDto> fetchLatestExchangeRates() {
        try {
            // Fetch only the latest observation for all currencies
            String url = buildApiUrl(1);

            log.info("Fetching latest exchange rates from: {}", url);
            String csvData = restTemplate.getForObject(url, String.class);

            if (csvData == null || csvData.isEmpty()) {
                log.warn("No data received from Bundesbank API");
                return Collections.emptyList();
            }

            List<ExchangeRateDataDto> result = parseAllCurrenciesData(csvData);
            log.info("Successfully fetched {} latest exchange rate records", result.size());
            return result;

        } catch (Exception e) {
            throw new ExternalApiException("Failed to fetch latest exchange rates from Bundesbank API", e);
        }
    }

    /**
     * Builds the API URL using Spring's UriComponentsBuilder for type-safe URL construction.
     */
    private String buildApiUrl(int observations) {
        return UriComponentsBuilder
                .fromUriString(apiConfig.getBaseUrl())
                .path("/{endpoint}")
                .queryParam("format", apiConfig.getFormat())
                .queryParam("lang", apiConfig.getLanguage())
                .queryParam("lastNObservations", observations)
                .buildAndExpand(apiConfig.getEndpoint().getAllCurrencies())
                .toUriString();
    }

    /**
     * Splits a CSV line respecting quoted fields.
     * Handles commas within quoted strings properly.
     */
    private String[] splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        result.add(current.toString());
        return result.toArray(new String[0]);
    }
}
