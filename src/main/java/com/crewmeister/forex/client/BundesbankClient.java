package com.crewmeister.forex.client;

import com.crewmeister.forex.config.BundesbankApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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

    private final BundesbankApiConfig apiConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Fetches exchange rates for all currencies from Bundesbank API in a single call.
     * Uses wildcard pattern to get all available currencies.
     * Returns a list of ExchangeRateDataDto with all CSV columns.
     */
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
            log.error("Error fetching exchange rates: {}", e.getMessage(), e);
            return Collections.emptyList();
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
                if (parts.length >= 9) {
                    try {
                        // Column 1: Frequency (D = Daily)
                        String frequency = parts[1].trim();

                        // Column 2: Currency code (e.g., AUD, USD)
                        String targetCurrency = parts[2].trim();

                        // Column 3: Partner currency (EUR)
                        String sourceCurrency = parts[3].trim();

                        // Column 7: Date (TIME_PERIOD)
                        String dateStr = parts[7].trim();

                        // Column 8: Rate (OBS_VALUE)
                        String valueStr = parts[8].trim();

                        // Column 18: Diff percent (BBK_DIFF) - if available
                        String diffStr = parts.length > 18 ? parts[18].trim() : "";

                        // Last column: Observation status (OBS_STATUS)
                        String obsStatus = parts.length > 19 ? parts[parts.length - 1].trim() : "";

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
     */
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
            log.error("Error fetching latest exchange rates: {}", e.getMessage(), e);
            return Collections.emptyList();
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
