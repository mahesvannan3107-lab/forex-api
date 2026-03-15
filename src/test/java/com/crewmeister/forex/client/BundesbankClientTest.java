package com.crewmeister.forex.client;

import com.crewmeister.forex.config.BundesbankApiConfig;
import com.crewmeister.forex.dto.ExchangeRateDataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class BundesbankClientTest {

    @Mock
    private BundesbankApiConfig apiConfig;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private BundesbankClient bundesbankClient;

    private BundesbankApiConfig.Endpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new BundesbankApiConfig.Endpoint();
        endpoint.setAllCurrencies("D..EUR.BB.AC.000");

        lenient().when(apiConfig.getBaseUrl()).thenReturn("https://api.statistiken.bundesbank.de/rest/data/BBEX3");
        lenient().when(apiConfig.getFormat()).thenReturn("sdmx_csv");
        lenient().when(apiConfig.getLanguage()).thenReturn("en");
        lenient().when(apiConfig.getDefaultObservations()).thenReturn(100);
        lenient().when(apiConfig.getEndpoint()).thenReturn(endpoint);
    }

    @Test
    void fetchAllExchangeRates_ValidResponse_ReturnsData() {
        String csvResponse = """
                DATAFLOW,FREQ,CURRENCY,PARTNER_CURRENCY,EXCH_TYPE,EXCH_RATE_TYPE,DATA_TYPE,TIME_PERIOD,OBS_VALUE,OBS_STATUS,BBK_DIFF
                BBEX3,D,USD,EUR,BB,AC,000,2026-03-01,1.1641,A,0.5
                BBEX3,D,GBP,EUR,BB,AC,000,2026-03-01,0.8590,A,-0.2
                """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(csvResponse);

        List<ExchangeRateDataDto> result = bundesbankClient.fetchAllExchangeRates();

        assertThat(result).isNotEmpty();
        assertThat(result).hasSizeGreaterThanOrEqualTo(1);
        verify(restTemplate).getForObject(anyString(), eq(String.class));
    }

    @Test
    void fetchAllExchangeRates_EmptyResponse_ReturnsEmptyList() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("");

        List<ExchangeRateDataDto> result = bundesbankClient.fetchAllExchangeRates();

        assertThat(result).isEmpty();
    }

    @Test
    void fetchAllExchangeRates_NullResponse_ReturnsEmptyList() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(null);

        List<ExchangeRateDataDto> result = bundesbankClient.fetchAllExchangeRates();

        assertThat(result).isEmpty();
    }

    @Test
    void fetchAllExchangeRates_ExceptionThrown_PropagatesRestClientException() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RestClientException("API Error"));

        assertThatThrownBy(() -> bundesbankClient.fetchAllExchangeRates())
                .isInstanceOf(RestClientException.class)
                .hasMessageContaining("API Error");
    }

    @Test
    void fetchLatestExchangeRates_ValidResponse_ReturnsData() {
        String csvResponse = """
                DATAFLOW,FREQ,CURRENCY,PARTNER_CURRENCY,EXCH_TYPE,EXCH_RATE_TYPE,DATA_TYPE,TIME_PERIOD,OBS_VALUE,OBS_STATUS,BBK_DIFF
                BBEX3,D,USD,EUR,BB,AC,000,2026-03-13,1.1650,A,0.1
                """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(csvResponse);

        List<ExchangeRateDataDto> result = bundesbankClient.fetchLatestExchangeRates();

        assertThat(result).isNotEmpty();
        verify(restTemplate).getForObject(anyString(), eq(String.class));
    }

    @Test
    void fetchLatestExchangeRates_EmptyResponse_ReturnsEmptyList() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("");

        List<ExchangeRateDataDto> result = bundesbankClient.fetchLatestExchangeRates();

        assertThat(result).isEmpty();
    }

    @Test
    void fetchLatestExchangeRates_ExceptionThrown_PropagatesRestClientException() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RestClientException("Network error"));

        assertThatThrownBy(() -> bundesbankClient.fetchLatestExchangeRates())
                .isInstanceOf(RestClientException.class)
                .hasMessageContaining("Network error");
    }

    @Test
    void fetchAllExchangeRates_ParsesMultipleCurrencies() {
        String csvResponse = """
                DATAFLOW,FREQ,CURRENCY,PARTNER_CURRENCY,EXCH_TYPE,EXCH_RATE_TYPE,DATA_TYPE,TIME_PERIOD,OBS_VALUE,OBS_STATUS,BBK_DIFF
                BBEX3,D,USD,EUR,BB,AC,000,2026-03-01,1.1641,A,0.5
                BBEX3,D,GBP,EUR,BB,AC,000,2026-03-01,0.8590,A,-0.2
                BBEX3,D,JPY,EUR,BB,AC,000,2026-03-01,130.50,A,1.0
                """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(csvResponse);

        List<ExchangeRateDataDto> result = bundesbankClient.fetchAllExchangeRates();

        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void fetchAllExchangeRates_SkipsInvalidLines() {
        String csvResponse = """
                DATAFLOW,FREQ,CURRENCY,PARTNER_CURRENCY,EXCH_TYPE,EXCH_RATE_TYPE,DATA_TYPE,TIME_PERIOD,OBS_VALUE,OBS_STATUS,BBK_DIFF
                BBEX3,D,USD,EUR,BB,AC,000,2026-03-01,1.1641,A,0.5
                # This is a comment
                BBEX3,D,GBP,EUR,BB,AC,000,2026-03-01,.,A,-0.2
                BBEX3,D,JPY,EUR,BB,AC,000,2026-03-01,130.50,A,1.0
                """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(csvResponse);

        List<ExchangeRateDataDto> result = bundesbankClient.fetchAllExchangeRates();

        assertThat(result).isNotEmpty();
    }

    @Test
    void fetchAllExchangeRates_UsesConfiguredApiUrl() {
        String csvResponse = """
                DATAFLOW,FREQ,CURRENCY,PARTNER_CURRENCY,EXCH_TYPE,EXCH_RATE_TYPE,DATA_TYPE,TIME_PERIOD,OBS_VALUE,OBS_STATUS,BBK_DIFF
                BBEX3,D,USD,EUR,BB,AC,000,2026-03-01,1.1641,A,0.5
                """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(csvResponse);

        bundesbankClient.fetchAllExchangeRates();

        verify(apiConfig, atLeastOnce()).getBaseUrl();
        verify(apiConfig, atLeastOnce()).getFormat();
        verify(apiConfig, atLeastOnce()).getLanguage();
        verify(apiConfig, atLeastOnce()).getDefaultObservations();
    }
}
