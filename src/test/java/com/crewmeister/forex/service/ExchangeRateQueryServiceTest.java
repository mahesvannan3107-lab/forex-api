package com.crewmeister.forex.service;

import com.crewmeister.forex.dto.ExchangeRateDto;
import com.crewmeister.forex.dto.ExchangeRatesByDateDto;
import com.crewmeister.forex.entity.ExchangeRate;
import com.crewmeister.forex.mapper.ExchangeRateMapper;
import com.crewmeister.forex.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExchangeRateQueryService pagination functionality.
 */
@ExtendWith(MockitoExtension.class)
class ExchangeRateQueryServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private ExchangeRateMapper exchangeRateMapper;

    @InjectMocks
    private ExchangeRateQueryService exchangeRateQueryService;

    private ExchangeRate testRate1;
    private ExchangeRate testRate2;
    private ExchangeRate testRate3;
    private ExchangeRateDto testRateDto1;
    private ExchangeRateDto testRateDto2;
    private ExchangeRateDto testRateDto3;

    @BeforeEach
    void setUp() {
        testRate1 = new ExchangeRate("EUR", "USD", LocalDate.of(2026, 3, 1), new BigDecimal("1.10"));
        testRate2 = new ExchangeRate("EUR", "GBP", LocalDate.of(2026, 3, 2), new BigDecimal("0.85"));
        testRate3 = new ExchangeRate("EUR", "JPY", LocalDate.of(2026, 3, 3), new BigDecimal("130.50"));

        testRateDto1 = new ExchangeRateDto("EUR/USD", "EUR", "USD", LocalDate.of(2026, 3, 1), new BigDecimal("1.10"), "1 EUR = 1.10 USD");
        testRateDto2 = new ExchangeRateDto("EUR/GBP", "EUR", "GBP", LocalDate.of(2026, 3, 2), new BigDecimal("0.85"), "1 EUR = 0.85 GBP");
        testRateDto3 = new ExchangeRateDto("EUR/JPY", "EUR", "JPY", LocalDate.of(2026, 3, 3), new BigDecimal("130.50"), "1 EUR = 130.50 JPY");
    }

    @Test
    void getExchangeRatesFromGroupedPaginated_FirstPage_ReturnsPagedResults() {
        Pageable pageable = PageRequest.of(0, 2);
        LocalDate date1 = LocalDate.of(2026, 3, 1);
        LocalDate date2 = LocalDate.of(2026, 3, 2);
        
        Page<LocalDate> datesPage = new PageImpl<>(Arrays.asList(date1, date2), pageable, 3);
        List<ExchangeRate> rates = Arrays.asList(testRate1, testRate2);

        when(exchangeRateRepository.findDistinctDatesBySourceCurrency("EUR", pageable))
                .thenReturn(datesPage);
        when(exchangeRateRepository.findBySourceCurrencyAndDateIn(eq("EUR"), any()))
                .thenReturn(rates);

        Page<ExchangeRatesByDateDto> result = exchangeRateQueryService.getExchangeRatesFromGroupedPaginated("EUR", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isFalse();
        assertThat(result.getContent().get(0).getBaseCurrency()).isEqualTo("EUR");

        verify(exchangeRateRepository).findDistinctDatesBySourceCurrency("EUR", pageable);
    }

    @Test
    void getExchangeRatesFromGroupedPaginated_LastPage_ReturnsPagedResults() {
        Pageable pageable = PageRequest.of(1, 2);
        LocalDate date3 = LocalDate.of(2026, 3, 3);
        
        Page<LocalDate> datesPage = new PageImpl<>(List.of(date3), pageable, 3);
        List<ExchangeRate> rates = List.of(testRate3);

        when(exchangeRateRepository.findDistinctDatesBySourceCurrency("EUR", pageable))
                .thenReturn(datesPage);
        when(exchangeRateRepository.findBySourceCurrencyAndDateIn(eq("EUR"), any()))
                .thenReturn(rates);

        Page<ExchangeRatesByDateDto> result = exchangeRateQueryService.getExchangeRatesFromGroupedPaginated("EUR", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isTrue();
    }

    @Test
    void getExchangeRatesFromGroupedPaginated_EmptyPage_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<LocalDate> emptyPage = Page.empty(pageable);

        when(exchangeRateRepository.findDistinctDatesBySourceCurrency("EUR", pageable))
                .thenReturn(emptyPage);

        Page<ExchangeRatesByDateDto> result = exchangeRateQueryService.getExchangeRatesFromGroupedPaginated("EUR", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getExchangeRatesFromGroupedPaginated_ConvertsToUpperCase() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDate date1 = LocalDate.of(2026, 3, 1);
        Page<LocalDate> datesPage = new PageImpl<>(List.of(date1), pageable, 1);

        when(exchangeRateRepository.findDistinctDatesBySourceCurrency("EUR", pageable))
                .thenReturn(datesPage);
        when(exchangeRateRepository.findBySourceCurrencyAndDateIn(eq("EUR"), any()))
                .thenReturn(List.of(testRate1));

        exchangeRateQueryService.getExchangeRatesFromGroupedPaginated("eur", pageable);

        verify(exchangeRateRepository).findDistinctDatesBySourceCurrency("EUR", pageable);
    }

    @Test
    void getExchangeRateHistoryPaginated_DirectPair_ReturnsPagedResults() {
        Pageable pageable = PageRequest.of(0, 2);
        List<ExchangeRate> rates = Arrays.asList(testRate1, testRate2);
        Page<ExchangeRate> ratePage = new PageImpl<>(rates, pageable, 2);

        when(exchangeRateRepository.findBySourceCurrencyAndTargetCurrencyOrderByDateDesc("EUR", "USD", pageable))
                .thenReturn(ratePage);
        when(exchangeRateMapper.toDto(testRate1)).thenReturn(testRateDto1);
        when(exchangeRateMapper.toDto(testRate2)).thenReturn(testRateDto2);

        Page<ExchangeRateDto> result = exchangeRateQueryService.getExchangeRateHistoryPaginated("EUR", "USD", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(exchangeRateRepository).findBySourceCurrencyAndTargetCurrencyOrderByDateDesc("EUR", "USD", pageable);
    }

    @Test
    void getExchangeRateHistoryPaginated_InversePair_ReturnsInverseRates() {
        Pageable pageable = PageRequest.of(0, 2);
        Page<ExchangeRate> emptyPage = Page.empty(pageable);
        
        ExchangeRate inverseRate = new ExchangeRate("USD", "EUR", LocalDate.of(2026, 3, 1), new BigDecimal("0.90"));
        Page<ExchangeRate> inverseRatePage = new PageImpl<>(List.of(inverseRate), pageable, 1);

        when(exchangeRateRepository.findBySourceCurrencyAndTargetCurrencyOrderByDateDesc("EUR", "USD", pageable))
                .thenReturn(emptyPage);
        when(exchangeRateRepository.findBySourceCurrencyAndTargetCurrencyOrderByDateDesc("USD", "EUR", pageable))
                .thenReturn(inverseRatePage);
        when(exchangeRateMapper.toDto(any(ExchangeRate.class))).thenReturn(testRateDto1);

        Page<ExchangeRateDto> result = exchangeRateQueryService.getExchangeRateHistoryPaginated("EUR", "USD", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(exchangeRateRepository).findBySourceCurrencyAndTargetCurrencyOrderByDateDesc("EUR", "USD", pageable);
        verify(exchangeRateRepository).findBySourceCurrencyAndTargetCurrencyOrderByDateDesc("USD", "EUR", pageable);
    }

    @Test
    void getExchangeRateHistoryPaginated_EmptyResult_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<ExchangeRate> emptyPage = Page.empty(pageable);

        when(exchangeRateRepository.findBySourceCurrencyAndTargetCurrencyOrderByDateDesc("EUR", "USD", pageable))
                .thenReturn(emptyPage);
        when(exchangeRateRepository.findBySourceCurrencyAndTargetCurrencyOrderByDateDesc("USD", "EUR", pageable))
                .thenReturn(emptyPage);

        Page<ExchangeRateDto> result = exchangeRateQueryService.getExchangeRateHistoryPaginated("EUR", "USD", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getExchangeRateHistoryPaginated_LargePageSize_HandlesCorrectly() {
        Pageable pageable = PageRequest.of(0, 100);
        List<ExchangeRate> rates = List.of(testRate1);
        Page<ExchangeRate> ratePage = new PageImpl<>(rates, pageable, 1);

        when(exchangeRateRepository.findBySourceCurrencyAndTargetCurrencyOrderByDateDesc("EUR", "USD", pageable))
                .thenReturn(ratePage);
        when(exchangeRateMapper.toDto(any())).thenReturn(testRateDto1);

        Page<ExchangeRateDto> result = exchangeRateQueryService.getExchangeRateHistoryPaginated("EUR", "USD", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getSize()).isEqualTo(100);
    }
}
