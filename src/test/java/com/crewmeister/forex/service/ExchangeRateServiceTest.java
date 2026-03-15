package com.crewmeister.forex.service;

import com.crewmeister.forex.dto.ConversionDto;
import com.crewmeister.forex.dto.ExchangeRateDto;
import com.crewmeister.forex.dto.ExchangeRatesByDateDto;
import com.crewmeister.forex.exception.InvalidParameterException;
import com.crewmeister.forex.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExchangeRateService (facade).
 * Tests delegation to underlying services.
 */
@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private IExchangeRateQueryService exchangeRateQueryService;

    @Mock
    private ICurrencyConversionService currencyConversionService;

    @Mock
    private IExchangeRateSyncService exchangeRateSyncService;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    private ExchangeRateDto testRateDto;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2026, 3, 1);
        testRateDto = new ExchangeRateDto(
                "EUR/USD", "EUR", "USD", testDate, 
                new BigDecimal("1.1641"), "1 EUR = 1.1641 USD"
        );
    }

    @Test
    void getExchangeRate_WithValidPair_ReturnsRate() {
        when(exchangeRateQueryService.getExchangeRate("EUR", "USD", null))
                .thenReturn(testRateDto);

        ExchangeRateDto result = exchangeRateService.getExchangeRate("EUR", "USD", null);

        assertThat(result).isNotNull();
        assertThat(result.getBaseCurrency()).isEqualTo("EUR");
        assertThat(result.getTargetCurrency()).isEqualTo("USD");
        assertThat(result.getRate()).isEqualByComparingTo("1.1641");
        verify(exchangeRateQueryService).getExchangeRate("EUR", "USD", null);
    }

    @Test
    void getExchangeRate_WithSpecificDate_ReturnsRateForDate() {
        when(exchangeRateQueryService.getExchangeRate("EUR", "USD", testDate))
                .thenReturn(testRateDto);

        ExchangeRateDto result = exchangeRateService.getExchangeRate("EUR", "USD", testDate);

        assertThat(result).isNotNull();
        assertThat(result.getDate()).isEqualTo(testDate);
        verify(exchangeRateQueryService).getExchangeRate("EUR", "USD", testDate);
    }

    @Test
    void getExchangeRate_WithInversePair_CalculatesInverseRate() {
        ExchangeRateDto inverseRateDto = new ExchangeRateDto(
                "EUR/USD", "EUR", "USD", testDate,
                new BigDecimal("1.1641"), "1 EUR = 1.1641 USD"
        );
        
        when(exchangeRateQueryService.getExchangeRate("EUR", "USD", null))
                .thenReturn(inverseRateDto);

        ExchangeRateDto result = exchangeRateService.getExchangeRate("EUR", "USD", null);

        assertThat(result).isNotNull();
        assertThat(result.getBaseCurrency()).isEqualTo("EUR");
        assertThat(result.getTargetCurrency()).isEqualTo("USD");
    }

    @Test
    void getExchangeRate_NotFound_ThrowsException() {
        when(exchangeRateQueryService.getExchangeRate(anyString(), anyString(), any()))
                .thenThrow(new ResourceNotFoundException("Exchange rate not found for EUR to XXX"));

        assertThatThrownBy(() -> exchangeRateService.getExchangeRate("EUR", "XXX", null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Exchange rate not found");
    }

    @Test
    void getExchangeRatesFromGrouped_Latest_ReturnsLatestRates() {
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("USD", new BigDecimal("1.1641"));
        rates.put("GBP", new BigDecimal("0.8590"));
        ExchangeRatesByDateDto groupedDto = new ExchangeRatesByDateDto(testDate, "EUR", rates);
        
        when(exchangeRateQueryService.getExchangeRatesFromGrouped("EUR", null))
                .thenReturn(Arrays.asList(groupedDto));

        List<ExchangeRatesByDateDto> result = exchangeRateService.getExchangeRatesFromGrouped("EUR", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBaseCurrency()).isEqualTo("EUR");
        assertThat(result.get(0).getRates()).hasSize(2);
    }

    @Test
    void getExchangeRatesFromGrouped_SpecificDate_ReturnsRatesForDate() {
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("USD", new BigDecimal("1.1641"));
        ExchangeRatesByDateDto groupedDto = new ExchangeRatesByDateDto(testDate, "EUR", rates);
        
        when(exchangeRateQueryService.getExchangeRatesFromGrouped("EUR", testDate))
                .thenReturn(Arrays.asList(groupedDto));

        List<ExchangeRatesByDateDto> result = exchangeRateService.getExchangeRatesFromGrouped("EUR", testDate);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDate()).isEqualTo(testDate);
    }

    @Test
    void getExchangeRatesFromGroupedHistory_ReturnsAllDates() {
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("USD", new BigDecimal("1.1641"));
        ExchangeRatesByDateDto groupedDto = new ExchangeRatesByDateDto(testDate, "EUR", rates);
        
        when(exchangeRateQueryService.getExchangeRatesFromGroupedHistory("EUR"))
                .thenReturn(Arrays.asList(groupedDto));

        List<ExchangeRatesByDateDto> result = exchangeRateService.getExchangeRatesFromGroupedHistory("EUR");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBaseCurrency()).isEqualTo("EUR");
        verify(exchangeRateQueryService).getExchangeRatesFromGroupedHistory("EUR");
    }

    @Test
    void convertCurrency_ValidParams_ReturnsConversion() {
        ConversionDto conversionDto = new ConversionDto(
                "EUR", new BigDecimal("100"), "USD", 
                new BigDecimal("116.41"), testDate, new BigDecimal("1.1641")
        );
        
        when(currencyConversionService.convertCurrency("EUR", "USD", new BigDecimal("100"), null))
                .thenReturn(conversionDto);

        ConversionDto result = exchangeRateService.convertCurrency("EUR", "USD", new BigDecimal("100"), null);

        assertThat(result).isNotNull();
        assertThat(result.getFromCurrency()).isEqualTo("EUR");
        assertThat(result.getToCurrency()).isEqualTo("USD");
        assertThat(result.getFromAmount()).isEqualByComparingTo("100");
        assertThat(result.getToAmount()).isEqualByComparingTo("116.41");
        assertThat(result.getExchangeRate()).isEqualByComparingTo("1.1641");
    }

    @Test
    void convertCurrency_NullFromCurrency_ThrowsException() {
        when(currencyConversionService.convertCurrency(null, "USD", new BigDecimal("100"), null))
                .thenThrow(new InvalidParameterException("'from' currency is required"));

        assertThatThrownBy(() -> exchangeRateService.convertCurrency(null, "USD", new BigDecimal("100"), null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'from' currency is required");
    }

    @Test
    void convertCurrency_BlankFromCurrency_ThrowsException() {
        when(currencyConversionService.convertCurrency("", "USD", new BigDecimal("100"), null))
                .thenThrow(new InvalidParameterException("'from' currency is required"));

        assertThatThrownBy(() -> exchangeRateService.convertCurrency("", "USD", new BigDecimal("100"), null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'from' currency is required");
    }

    @Test
    void convertCurrency_NullToCurrency_ThrowsException() {
        when(currencyConversionService.convertCurrency("EUR", null, new BigDecimal("100"), null))
                .thenThrow(new InvalidParameterException("'to' currency is required"));

        assertThatThrownBy(() -> exchangeRateService.convertCurrency("EUR", null, new BigDecimal("100"), null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'to' currency is required");
    }

    @Test
    void convertCurrency_InvalidFromCurrencyCode_ThrowsException() {
        when(currencyConversionService.convertCurrency("EU", "USD", new BigDecimal("100"), null))
                .thenThrow(new InvalidParameterException("'from' must be a valid 3-letter currency code"));

        assertThatThrownBy(() -> exchangeRateService.convertCurrency("EU", "USD", new BigDecimal("100"), null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'from' must be a valid 3-letter currency code");
    }

    @Test
    void convertCurrency_InvalidToCurrencyCode_ThrowsException() {
        when(currencyConversionService.convertCurrency("EUR", "US", new BigDecimal("100"), null))
                .thenThrow(new InvalidParameterException("'to' must be a valid 3-letter currency code"));

        assertThatThrownBy(() -> exchangeRateService.convertCurrency("EUR", "US", new BigDecimal("100"), null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'to' must be a valid 3-letter currency code");
    }

    @Test
    void convertCurrency_SameCurrency_ThrowsException() {
        when(currencyConversionService.convertCurrency("EUR", "EUR", new BigDecimal("100"), null))
                .thenThrow(new InvalidParameterException("'from' and 'to' currencies must be different"));

        assertThatThrownBy(() -> exchangeRateService.convertCurrency("EUR", "EUR", new BigDecimal("100"), null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'from' and 'to' currencies must be different");
    }

    @Test
    void convertCurrency_NullAmount_ThrowsException() {
        when(currencyConversionService.convertCurrency("EUR", "USD", null, null))
                .thenThrow(new InvalidParameterException("'amount' is required"));

        assertThatThrownBy(() -> exchangeRateService.convertCurrency("EUR", "USD", null, null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'amount' is required");
    }

    @Test
    void convertCurrency_ZeroAmount_ThrowsException() {
        when(currencyConversionService.convertCurrency("EUR", "USD", BigDecimal.ZERO, null))
                .thenThrow(new InvalidParameterException("'amount' must be greater than zero"));

        assertThatThrownBy(() -> exchangeRateService.convertCurrency("EUR", "USD", BigDecimal.ZERO, null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'amount' must be greater than zero");
    }

    @Test
    void convertCurrency_NegativeAmount_ThrowsException() {
        when(currencyConversionService.convertCurrency("EUR", "USD", new BigDecimal("-100"), null))
                .thenThrow(new InvalidParameterException("'amount' must be greater than zero"));

        assertThatThrownBy(() -> exchangeRateService.convertCurrency("EUR", "USD", new BigDecimal("-100"), null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'amount' must be greater than zero");
    }

    @Test
    void convertCurrency_FutureDate_ThrowsException() {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        
        when(currencyConversionService.convertCurrency("EUR", "USD", new BigDecimal("100"), futureDate))
                .thenThrow(new InvalidParameterException("'date' cannot be a future date"));

        assertThatThrownBy(() -> exchangeRateService.convertCurrency("EUR", "USD", new BigDecimal("100"), futureDate))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'date' cannot be a future date");
    }

    @Test
    void getExchangeRateHistory_ValidPair_ReturnsHistory() {
        ExchangeRateDto rate1 = new ExchangeRateDto(
                "EUR/USD", "EUR", "USD", testDate,
                new BigDecimal("1.1641"), "1 EUR = 1.1641 USD"
        );
        ExchangeRateDto rate2 = new ExchangeRateDto(
                "EUR/USD", "EUR", "USD", testDate.minusDays(1),
                new BigDecimal("1.1600"), "1 EUR = 1.1600 USD"
        );
        
        when(exchangeRateQueryService.getExchangeRateHistory("EUR", "USD"))
                .thenReturn(Arrays.asList(rate1, rate2));

        List<ExchangeRateDto> result = exchangeRateService.getExchangeRateHistory("EUR", "USD");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDate()).isEqualTo(testDate);
        assertThat(result.get(1).getDate()).isEqualTo(testDate.minusDays(1));
    }

    @Test
    void syncExchangeRates_DelegatesToSyncService() {
        doNothing().when(exchangeRateSyncService).syncExchangeRates();

        exchangeRateService.syncExchangeRates();

        verify(exchangeRateSyncService).syncExchangeRates();
    }

    @Test
    void syncLatestExchangeRates_DelegatesToSyncService() {
        when(exchangeRateSyncService.syncLatestExchangeRates()).thenReturn(10);

        int result = exchangeRateService.syncLatestExchangeRates();

        assertThat(result).isEqualTo(10);
        verify(exchangeRateSyncService).syncLatestExchangeRates();
    }
}
