package com.crewmeister.forex.service;

import com.crewmeister.forex.client.BundesbankClient;
import com.crewmeister.forex.dto.ConversionDto;
import com.crewmeister.forex.dto.ExchangeRateDto;
import com.crewmeister.forex.dto.ExchangeRatesByDateDto;
import com.crewmeister.forex.entity.ExchangeRate;
import com.crewmeister.forex.exception.InvalidParameterException;
import com.crewmeister.forex.exception.ResourceNotFoundException;
import com.crewmeister.forex.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private BundesbankClient bundesbankClient;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    private ExchangeRate testRate;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2026, 3, 1);
        testRate = new ExchangeRate("EUR", "USD", testDate, new BigDecimal("1.1641"));
    }

    @Test
    void getExchangeRate_WithValidPair_ReturnsRate() {
        when(exchangeRateRepository.findLatestRateBySourceAndTarget("EUR", "USD"))
                .thenReturn(Optional.of(testRate));

        ExchangeRateDto result = exchangeRateService.getExchangeRate("EUR", "USD", null);

        assertThat(result).isNotNull();
        assertThat(result.getBaseCurrency()).isEqualTo("EUR");
        assertThat(result.getTargetCurrency()).isEqualTo("USD");
        assertThat(result.getRate()).isEqualByComparingTo("1.1641");
        verify(exchangeRateRepository).findLatestRateBySourceAndTarget("EUR", "USD");
    }

    @Test
    void getExchangeRate_WithSpecificDate_ReturnsRateForDate() {
        when(exchangeRateRepository.findBySourceCurrencyAndTargetCurrencyAndDate("EUR", "USD", testDate))
                .thenReturn(Optional.of(testRate));

        ExchangeRateDto result = exchangeRateService.getExchangeRate("EUR", "USD", testDate);

        assertThat(result).isNotNull();
        assertThat(result.getDate()).isEqualTo(testDate);
        verify(exchangeRateRepository).findBySourceCurrencyAndTargetCurrencyAndDate("EUR", "USD", testDate);
    }

    @Test
    void getExchangeRate_WithInversePair_CalculatesInverseRate() {
        ExchangeRate inverseRate = new ExchangeRate("USD", "EUR", testDate, new BigDecimal("0.8590"));
        
        when(exchangeRateRepository.findLatestRateBySourceAndTarget("EUR", "USD"))
                .thenReturn(Optional.empty());
        when(exchangeRateRepository.findLatestRateBySourceAndTarget("USD", "EUR"))
                .thenReturn(Optional.of(inverseRate));

        ExchangeRateDto result = exchangeRateService.getExchangeRate("EUR", "USD", null);

        assertThat(result).isNotNull();
        assertThat(result.getBaseCurrency()).isEqualTo("EUR");
        assertThat(result.getTargetCurrency()).isEqualTo("USD");
    }

    @Test
    void getExchangeRate_NotFound_ThrowsException() {
        when(exchangeRateRepository.findLatestRateBySourceAndTarget(anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> exchangeRateService.getExchangeRate("EUR", "XXX", null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Exchange rate not found");
    }

    @Test
    void getExchangeRatesFromGrouped_Latest_ReturnsLatestRates() {
        ExchangeRate rate1 = new ExchangeRate("EUR", "USD", testDate, new BigDecimal("1.1641"));
        ExchangeRate rate2 = new ExchangeRate("EUR", "GBP", testDate, new BigDecimal("0.8590"));
        
        when(exchangeRateRepository.findLatestRatesBySourceCurrency("EUR"))
                .thenReturn(Arrays.asList(rate1, rate2));

        List<ExchangeRatesByDateDto> result = exchangeRateService.getExchangeRatesFromGrouped("EUR", null, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBaseCurrency()).isEqualTo("EUR");
        assertThat(result.get(0).getRates()).hasSize(2);
    }

    @Test
    void getExchangeRatesFromGrouped_SpecificDate_ReturnsRatesForDate() {
        ExchangeRate rate1 = new ExchangeRate("EUR", "USD", testDate, new BigDecimal("1.1641"));
        
        when(exchangeRateRepository.findBySourceCurrencyAndDate("EUR", testDate))
                .thenReturn(Arrays.asList(rate1));

        List<ExchangeRatesByDateDto> result = exchangeRateService.getExchangeRatesFromGrouped("EUR", testDate, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDate()).isEqualTo(testDate);
    }

    @Test
    void convertCurrency_ValidParams_ReturnsConversion() {
        when(exchangeRateRepository.findLatestRateBySourceAndTarget("EUR", "USD"))
                .thenReturn(Optional.of(testRate));

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
        assertThatThrownBy(() -> exchangeRateService.convertCurrency(null, "USD", new BigDecimal("100"), null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'from' currency is required");
    }

    @Test
    void convertCurrency_BlankFromCurrency_ThrowsException() {
        assertThatThrownBy(() -> exchangeRateService.convertCurrency("", "USD", new BigDecimal("100"), null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'from' currency is required");
    }

    @Test
    void convertCurrency_NullToCurrency_ThrowsException() {
        assertThatThrownBy(() -> exchangeRateService.convertCurrency("EUR", null, new BigDecimal("100"), null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'to' currency is required");
    }

    @Test
    void convertCurrency_InvalidFromCurrencyCode_ThrowsException() {
        assertThatThrownBy(() -> exchangeRateService.convertCurrency("EU", "USD", new BigDecimal("100"), null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'from' must be a valid 3-letter currency code");
    }

    @Test
    void convertCurrency_InvalidToCurrencyCode_ThrowsException() {
        assertThatThrownBy(() -> exchangeRateService.convertCurrency("EUR", "US", new BigDecimal("100"), null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'to' must be a valid 3-letter currency code");
    }

    @Test
    void convertCurrency_SameCurrency_ThrowsException() {
        assertThatThrownBy(() -> exchangeRateService.convertCurrency("EUR", "EUR", new BigDecimal("100"), null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'from' and 'to' currencies must be different");
    }

    @Test
    void convertCurrency_NullAmount_ThrowsException() {
        assertThatThrownBy(() -> exchangeRateService.convertCurrency("EUR", "USD", null, null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'amount' is required");
    }

    @Test
    void convertCurrency_ZeroAmount_ThrowsException() {
        assertThatThrownBy(() -> exchangeRateService.convertCurrency("EUR", "USD", BigDecimal.ZERO, null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'amount' must be greater than zero");
    }

    @Test
    void convertCurrency_NegativeAmount_ThrowsException() {
        assertThatThrownBy(() -> exchangeRateService.convertCurrency("EUR", "USD", new BigDecimal("-100"), null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'amount' must be greater than zero");
    }

    @Test
    void convertCurrency_FutureDate_ThrowsException() {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        
        assertThatThrownBy(() -> exchangeRateService.convertCurrency("EUR", "USD", new BigDecimal("100"), futureDate))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("'date' cannot be a future date");
    }

    @Test
    void getExchangeRateHistory_ValidPair_ReturnsHistory() {
        ExchangeRate rate1 = new ExchangeRate("EUR", "USD", testDate, new BigDecimal("1.1641"));
        ExchangeRate rate2 = new ExchangeRate("EUR", "USD", testDate.minusDays(1), new BigDecimal("1.1600"));
        
        when(exchangeRateRepository.findBySourceCurrencyAndTargetCurrencyOrderByDateDesc("EUR", "USD"))
                .thenReturn(Arrays.asList(rate1, rate2));

        List<ExchangeRateDto> result = exchangeRateService.getExchangeRateHistory("EUR", "USD", null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDate()).isEqualTo(testDate);
        assertThat(result.get(1).getDate()).isEqualTo(testDate.minusDays(1));
    }
}
