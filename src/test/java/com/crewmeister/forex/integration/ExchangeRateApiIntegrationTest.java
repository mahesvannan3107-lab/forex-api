package com.crewmeister.forex.integration;

import com.crewmeister.forex.config.DataLoader;
import com.crewmeister.forex.entity.Currency;
import com.crewmeister.forex.entity.ExchangeRate;
import com.crewmeister.forex.repository.CurrencyRepository;
import com.crewmeister.forex.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.sql.init.mode=never"
})
class ExchangeRateApiIntegrationTest {

    @MockBean
    private DataLoader dataLoader;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        exchangeRateRepository.deleteAll();
        currencyRepository.deleteAll();

        testDate = LocalDate.of(2026, 3, 1);

        Currency eur = new Currency();
        eur.setCode("EUR");
        eur.setName("Euro");
        currencyRepository.save(eur);

        Currency usd = new Currency();
        usd.setCode("USD");
        usd.setName("US Dollar");
        currencyRepository.save(usd);

        Currency gbp = new Currency();
        gbp.setCode("GBP");
        gbp.setName("British Pound");
        currencyRepository.save(gbp);

        ExchangeRate eurUsd = new ExchangeRate("EUR", "USD", testDate, new BigDecimal("1.1641"));
        exchangeRateRepository.save(eurUsd);

        ExchangeRate eurGbp = new ExchangeRate("EUR", "GBP", testDate, new BigDecimal("0.8590"));
        exchangeRateRepository.save(eurGbp);

        ExchangeRate usdEur = new ExchangeRate("USD", "EUR", testDate, new BigDecimal("0.8590"));
        exchangeRateRepository.save(usdEur);
    }

    @Test
    void getAllCurrencies_ReturnsAllCurrencies() throws Exception {
        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].code", containsInAnyOrder("EUR", "USD", "GBP")));
    }

    @Test
    void getExchangeRatesFromBase_Latest_ReturnsRates() throws Exception {
        mockMvc.perform(get("/api/forex-rates/EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].baseCurrency").value("EUR"))
                .andExpect(jsonPath("$[0].rates.USD").value(1.1641))
                .andExpect(jsonPath("$[0].rates.GBP").value(0.8590));
    }

    @Test
    void getExchangeRatesFromBase_SpecificDate_ReturnsRatesForDate() throws Exception {
        mockMvc.perform(get("/api/forex-rates/EUR")
                        .param("date", "2026-03-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-03-01"))
                .andExpect(jsonPath("$[0].baseCurrency").value("EUR"));
    }

    @Test
    void getExchangeRate_ValidPair_ReturnsRate() throws Exception {
        mockMvc.perform(get("/api/forex-rates/EUR/USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pair").value("EUR/USD"))
                .andExpect(jsonPath("$.baseCurrency").value("EUR"))
                .andExpect(jsonPath("$.targetCurrency").value("USD"))
                .andExpect(jsonPath("$.rate").value(1.1641));
    }

    @Test
    void getExchangeRate_InversePair_CalculatesInverse() throws Exception {
        mockMvc.perform(get("/api/forex-rates/USD/EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.targetCurrency").value("EUR"));
    }

    @Test
    void getExchangeRate_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/forex-rates/EUR/XXX"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void convertCurrency_ValidParams_ReturnsConversion() throws Exception {
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
    void convertCurrency_WithSpecificDate_ReturnsConversionForDate() throws Exception {
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
    void convertCurrency_InvalidAmount_Returns400() throws Exception {
        mockMvc.perform(get("/api/forex-rates/convert")
                        .param("from", "EUR")
                        .param("to", "USD")
                        .param("amount", "-100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'amount' must be greater than zero"));
    }

    @Test
    void convertCurrency_SameCurrency_Returns400() throws Exception {
        mockMvc.perform(get("/api/forex-rates/convert")
                        .param("from", "EUR")
                        .param("to", "EUR")
                        .param("amount", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'from' and 'to' currencies must be different"));
    }

    @Test
    void convertCurrency_InvalidCurrencyCode_Returns400() throws Exception {
        mockMvc.perform(get("/api/forex-rates/convert")
                        .param("from", "EU")
                        .param("to", "USD")
                        .param("amount", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'from' must be a valid 3-letter currency code"));
    }

    @Test
    void getExchangeRateHistory_ReturnsHistory() throws Exception {
        LocalDate yesterday = testDate.minusDays(1);
        ExchangeRate historicalRate = new ExchangeRate("EUR", "USD", yesterday, new BigDecimal("1.1600"));
        exchangeRateRepository.save(historicalRate);

        mockMvc.perform(get("/api/forex-rates/EUR/USD/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].date").value("2026-03-01"))
                .andExpect(jsonPath("$[1].date").value("2026-02-28"));
    }
}
