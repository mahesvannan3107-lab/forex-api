package com.crewmeister.forex.controller;

import com.crewmeister.forex.dto.CurrencyDto;
import com.crewmeister.forex.service.ICurrencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CurrencyController.class)
class CurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ICurrencyService currencyService;

    @Test
    void getAllCurrencies_ReturnsAllCurrencies() throws Exception {
        List<CurrencyDto> currencies = Arrays.asList(
                new CurrencyDto("EUR", "Euro"),
                new CurrencyDto("USD", "US Dollar"),
                new CurrencyDto("GBP", "British Pound")
        );

        when(currencyService.getAllCurrencies()).thenReturn(currencies);

        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].code").value("EUR"))
                .andExpect(jsonPath("$[0].name").value("Euro"))
                .andExpect(jsonPath("$[1].code").value("USD"))
                .andExpect(jsonPath("$[1].name").value("US Dollar"))
                .andExpect(jsonPath("$[2].code").value("GBP"))
                .andExpect(jsonPath("$[2].name").value("British Pound"));
    }

    @Test
    void getAllCurrencies_EmptyList_ReturnsEmptyArray() throws Exception {
        when(currencyService.getAllCurrencies()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllCurrencies_SingleCurrency_ReturnsSingleItem() throws Exception {
        List<CurrencyDto> currencies = Collections.singletonList(
                new CurrencyDto("INR", "Indian Rupee")
        );

        when(currencyService.getAllCurrencies()).thenReturn(currencies);

        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("INR"))
                .andExpect(jsonPath("$[0].name").value("Indian Rupee"));
    }

    @Test
    void getAllCurrencies_ReturnsJsonContentType() throws Exception {
        when(currencyService.getAllCurrencies()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }
}
