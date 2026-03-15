package com.crewmeister.forex.controller;

import com.crewmeister.forex.dto.ConversionDto;
import com.crewmeister.forex.dto.ExchangeRateDto;
import com.crewmeister.forex.dto.ExchangeRatesByDateDto;
import com.crewmeister.forex.exception.InvalidParameterException;
import com.crewmeister.forex.exception.ResourceNotFoundException;
import com.crewmeister.forex.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExchangeRateController.class)
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    // ==================== GET /{base} ====================

    @Test
    void getExchangeRatesFromBase_Latest_ReturnsRates() throws Exception {
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        rates.put("USD", new BigDecimal("1.1641"));
        rates.put("GBP", new BigDecimal("0.8590"));

        ExchangeRatesByDateDto dto = new ExchangeRatesByDateDto(
                LocalDate.of(2026, 3, 1),
                "EUR",
                rates
        );

        when(exchangeRateService.getExchangeRatesFromGrouped("EUR", null))
                .thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/forex-rates/EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].baseCurrency").value("EUR"))
                .andExpect(jsonPath("$[0].rates.USD").value(1.1641))
                .andExpect(jsonPath("$[0].rates.GBP").value(0.8590));
    }

    @Test
    void getExchangeRatesFromBase_SpecificDate_ReturnsRatesForDate() throws Exception {
        LocalDate testDate = LocalDate.of(2026, 3, 1);
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        rates.put("USD", new BigDecimal("1.1641"));

        ExchangeRatesByDateDto dto = new ExchangeRatesByDateDto(testDate, "EUR", rates);

        when(exchangeRateService.getExchangeRatesFromGrouped("EUR", testDate))
                .thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/forex-rates/EUR")
                        .param("date", "2026-03-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-03-01"));
    }

    @Test
    void getExchangeRatesFromBase_InvalidCurrencyCode_Returns400() throws Exception {
        mockMvc.perform(get("/api/forex-rates/INVALID"))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET /{base}/history ====================

    @Test
    void getExchangeRatesFromBaseHistory_ReturnsAllDates() throws Exception {
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        rates.put("USD", new BigDecimal("1.1641"));

        ExchangeRatesByDateDto dto = new ExchangeRatesByDateDto(
                LocalDate.of(2026, 3, 1),
                "EUR",
                rates
        );

        when(exchangeRateService.getExchangeRatesFromGroupedHistory("EUR"))
                .thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/forex-rates/EUR/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].baseCurrency").value("EUR"));
    }

    // ==================== GET /{base}/history/paginated ====================

    @Test
    void getExchangeRatesFromBaseHistoryPaginated_ReturnsPagedResults() throws Exception {
        Map<String, BigDecimal> rates1 = new LinkedHashMap<>();
        rates1.put("USD", new BigDecimal("1.1641"));
        rates1.put("GBP", new BigDecimal("0.8590"));

        Map<String, BigDecimal> rates2 = new LinkedHashMap<>();
        rates2.put("USD", new BigDecimal("1.1600"));
        rates2.put("GBP", new BigDecimal("0.8550"));

        ExchangeRatesByDateDto dto1 = new ExchangeRatesByDateDto(
                LocalDate.of(2026, 3, 1), "EUR", rates1);
        ExchangeRatesByDateDto dto2 = new ExchangeRatesByDateDto(
                LocalDate.of(2026, 2, 28), "EUR", rates2);

        Page<ExchangeRatesByDateDto> page = new PageImpl<>(
                Arrays.asList(dto1, dto2),
                PageRequest.of(0, 2),
                10
        );

        when(exchangeRateService.getExchangeRatesFromGroupedPaginated(eq("EUR"), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/forex-rates/EUR/history/paginated")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(5))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.content[0].baseCurrency").value("EUR"))
                .andExpect(jsonPath("$.content[0].rates.USD").value(1.1641));
    }

    @Test
    void getExchangeRatesFromBaseHistoryPaginated_LastPage_ReturnsPagedResults() throws Exception {
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        rates.put("USD", new BigDecimal("1.1641"));

        ExchangeRatesByDateDto dto = new ExchangeRatesByDateDto(
                LocalDate.of(2026, 3, 1), "EUR", rates);

        Page<ExchangeRatesByDateDto> page = new PageImpl<>(
                List.of(dto),
                PageRequest.of(4, 2),
                9
        );

        when(exchangeRateService.getExchangeRatesFromGroupedPaginated(eq("EUR"), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/forex-rates/EUR/history/paginated")
                        .param("page", "4")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.content[0].baseCurrency").value("EUR"));
    }

    @Test
    void getExchangeRatesFromBaseHistoryPaginated_DefaultParams_UsesDefaults() throws Exception {
        Page<ExchangeRatesByDateDto> page = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 20),
                0
        );

        when(exchangeRateService.getExchangeRatesFromGroupedPaginated(eq("EUR"), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/forex-rates/EUR/history/paginated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void getExchangeRatesFromBaseHistoryPaginated_InvalidCurrencyCode_Returns400() throws Exception {
        mockMvc.perform(get("/api/forex-rates/123/history/paginated"))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET /{base}/{target} ====================

    @Test
    void getExchangeRate_ValidPair_ReturnsRate() throws Exception {
        ExchangeRateDto dto = new ExchangeRateDto(
                "EUR/USD",
                "EUR",
                "USD",
                LocalDate.of(2026, 3, 1),
                new BigDecimal("1.1641"),
                "1 EUR = 1.1641 USD"
        );

        when(exchangeRateService.getExchangeRate("EUR", "USD", null))
                .thenReturn(dto);

        mockMvc.perform(get("/api/forex-rates/EUR/USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pair").value("EUR/USD"))
                .andExpect(jsonPath("$.baseCurrency").value("EUR"))
                .andExpect(jsonPath("$.targetCurrency").value("USD"))
                .andExpect(jsonPath("$.rate").value(1.1641));
    }

    @Test
    void getExchangeRate_WithDate_ReturnsRateForDate() throws Exception {
        LocalDate testDate = LocalDate.of(2026, 3, 1);
        ExchangeRateDto dto = new ExchangeRateDto(
                "EUR/USD",
                "EUR",
                "USD",
                testDate,
                new BigDecimal("1.1641"),
                "1 EUR = 1.1641 USD"
        );

        when(exchangeRateService.getExchangeRate("EUR", "USD", testDate))
                .thenReturn(dto);

        mockMvc.perform(get("/api/forex-rates/EUR/USD")
                        .param("date", "2026-03-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-03-01"));
    }

    @Test
    void getExchangeRate_NotFound_Returns404() throws Exception {
        when(exchangeRateService.getExchangeRate(anyString(), anyString(), any()))
                .thenThrow(new ResourceNotFoundException("Exchange rate not found"));

        mockMvc.perform(get("/api/forex-rates/EUR/XXX"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void getExchangeRate_InvalidCurrencyCode_Returns400() throws Exception {
        mockMvc.perform(get("/api/forex-rates/EU/USD"))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET /{base}/{target}/history ====================

    @Test
    void getExchangeRateHistory_ReturnsHistory() throws Exception {
        ExchangeRateDto dto1 = new ExchangeRateDto(
                "EUR/USD", "EUR", "USD",
                LocalDate.of(2026, 3, 1),
                new BigDecimal("1.1641"),
                "1 EUR = 1.1641 USD"
        );
        ExchangeRateDto dto2 = new ExchangeRateDto(
                "EUR/USD", "EUR", "USD",
                LocalDate.of(2026, 2, 28),
                new BigDecimal("1.1600"),
                "1 EUR = 1.1600 USD"
        );

        when(exchangeRateService.getExchangeRateHistory("EUR", "USD"))
                .thenReturn(Arrays.asList(dto1, dto2));

        mockMvc.perform(get("/api/forex-rates/EUR/USD/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].rate").value(1.1641))
                .andExpect(jsonPath("$[1].rate").value(1.1600));
    }

    // ==================== GET /{base}/{target}/history/paginated ====================

    @Test
    void getExchangeRateHistoryPaginated_ReturnsPagedResults() throws Exception {
        ExchangeRateDto dto1 = new ExchangeRateDto(
                "EUR/USD", "EUR", "USD",
                LocalDate.of(2026, 3, 1),
                new BigDecimal("1.1641"),
                "1 EUR = 1.1641 USD"
        );
        ExchangeRateDto dto2 = new ExchangeRateDto(
                "EUR/USD", "EUR", "USD",
                LocalDate.of(2026, 2, 28),
                new BigDecimal("1.1600"),
                "1 EUR = 1.1600 USD"
        );

        Page<ExchangeRateDto> page = new PageImpl<>(
                Arrays.asList(dto1, dto2),
                PageRequest.of(0, 2),
                100
        );

        when(exchangeRateService.getExchangeRateHistoryPaginated(eq("EUR"), eq("USD"), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/forex-rates/EUR/USD/history/paginated")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(100))
                .andExpect(jsonPath("$.totalPages").value(50))
                .andExpect(jsonPath("$.content[0].rate").value(1.1641))
                .andExpect(jsonPath("$.content[1].rate").value(1.1600));
    }

    // ==================== GET /convert ====================

    @Test
    void convertCurrency_ValidParams_ReturnsConversion() throws Exception {
        ConversionDto dto = new ConversionDto(
                "EUR",
                new BigDecimal("100"),
                "USD",
                new BigDecimal("116.41"),
                LocalDate.of(2026, 3, 1),
                new BigDecimal("1.1641")
        );

        when(exchangeRateService.convertCurrency("EUR", "USD", new BigDecimal("100"), null))
                .thenReturn(dto);

        mockMvc.perform(get("/api/forex-rates/convert")
                        .param("from", "EUR")
                        .param("to", "USD")
                        .param("amount", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromCurrency").value("EUR"))
                .andExpect(jsonPath("$.toCurrency").value("USD"))
                .andExpect(jsonPath("$.fromAmount").value(100))
                .andExpect(jsonPath("$.toAmount").value(116.41))
                .andExpect(jsonPath("$.exchangeRate").value(1.1641));
    }

    @Test
    void convertCurrency_WithDate_ReturnsConversionForDate() throws Exception {
        LocalDate testDate = LocalDate.of(2026, 3, 1);
        ConversionDto dto = new ConversionDto(
                "EUR",
                new BigDecimal("100"),
                "USD",
                new BigDecimal("116.41"),
                testDate,
                new BigDecimal("1.1641")
        );

        when(exchangeRateService.convertCurrency("EUR", "USD", new BigDecimal("100"), testDate))
                .thenReturn(dto);

        mockMvc.perform(get("/api/forex-rates/convert")
                        .param("from", "EUR")
                        .param("to", "USD")
                        .param("amount", "100")
                        .param("date", "2026-03-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-03-01"));
    }

    @Test
    void convertCurrency_MissingFromParam_Returns400() throws Exception {
        mockMvc.perform(get("/api/forex-rates/convert")
                        .param("to", "USD")
                        .param("amount", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("'from' parameter is required"));
    }

    @Test
    void convertCurrency_MissingToParam_Returns400() throws Exception {
        mockMvc.perform(get("/api/forex-rates/convert")
                        .param("from", "EUR")
                        .param("amount", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'to' parameter is required"));
    }

    @Test
    void convertCurrency_MissingAmountParam_Returns400() throws Exception {
        mockMvc.perform(get("/api/forex-rates/convert")
                        .param("from", "EUR")
                        .param("to", "USD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'amount' parameter is required"));
    }

    @Test
    void convertCurrency_InvalidAmount_Returns400() throws Exception {
        when(exchangeRateService.convertCurrency(anyString(), anyString(), any(), any()))
                .thenThrow(new InvalidParameterException("'amount' must be greater than zero"));

        mockMvc.perform(get("/api/forex-rates/convert")
                        .param("from", "EUR")
                        .param("to", "USD")
                        .param("amount", "-100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'amount' must be greater than zero"));
    }

    @Test
    void convertCurrency_SameCurrency_Returns400() throws Exception {
        when(exchangeRateService.convertCurrency(anyString(), anyString(), any(), any()))
                .thenThrow(new InvalidParameterException("'from' and 'to' currencies must be different"));

        mockMvc.perform(get("/api/forex-rates/convert")
                        .param("from", "EUR")
                        .param("to", "EUR")
                        .param("amount", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'from' and 'to' currencies must be different"));
    }
}
