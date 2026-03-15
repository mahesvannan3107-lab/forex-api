package com.crewmeister.forex.service;

import com.crewmeister.forex.dto.CurrencyDto;
import com.crewmeister.forex.entity.Currency;
import com.crewmeister.forex.mapper.CurrencyMapper;
import com.crewmeister.forex.repository.CurrencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @Spy
    private CurrencyMapper currencyMapper;

    @InjectMocks
    private CurrencyService currencyService;

    private List<Currency> testCurrencies;

    @BeforeEach
    void setUp() {
        Currency eur = new Currency();
        eur.setCode("EUR");
        eur.setName("Euro");

        Currency usd = new Currency();
        usd.setCode("USD");
        usd.setName("US Dollar");

        Currency gbp = new Currency();
        gbp.setCode("GBP");
        gbp.setName("British Pound");

        testCurrencies = Arrays.asList(eur, usd, gbp);
    }

    @Test
    void getAllCurrencies_ReturnsAllCurrencies() {
        when(currencyRepository.findAll()).thenReturn(testCurrencies);

        List<CurrencyDto> result = currencyService.getAllCurrencies();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).code()).isEqualTo("EUR");
        assertThat(result.get(0).name()).isEqualTo("Euro");
        assertThat(result.get(1).code()).isEqualTo("USD");
        assertThat(result.get(1).name()).isEqualTo("US Dollar");
        assertThat(result.get(2).code()).isEqualTo("GBP");
        assertThat(result.get(2).name()).isEqualTo("British Pound");
        
        verify(currencyRepository).findAll();
    }

    @Test
    void getAllCurrencies_EmptyRepository_ReturnsEmptyList() {
        when(currencyRepository.findAll()).thenReturn(Collections.emptyList());

        List<CurrencyDto> result = currencyService.getAllCurrencies();

        assertThat(result).isEmpty();
        verify(currencyRepository).findAll();
    }

    @Test
    void getAllCurrencies_VerifyDtoMapping() {
        Currency testCurrency = new Currency();
        testCurrency.setCode("INR");
        testCurrency.setName("Indian Rupee");

        when(currencyRepository.findAll()).thenReturn(Collections.singletonList(testCurrency));

        List<CurrencyDto> result = currencyService.getAllCurrencies();

        assertThat(result).hasSize(1);
        CurrencyDto dto = result.get(0);
        assertThat(dto.code()).isEqualTo("INR");
        assertThat(dto.name()).isEqualTo("Indian Rupee");
    }
}
