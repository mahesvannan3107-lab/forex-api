package com.crewmeister.forex.integration;

import com.crewmeister.forex.entity.ExchangeRate;
import com.crewmeister.forex.repository.ExchangeRateRepository;
import com.crewmeister.forex.scheduler.ScheduledExchangeRateUpdater;
import com.crewmeister.forex.service.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.crewmeister.forex.config.DataLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.sql.init.mode=never"
})
class SchedulerIntegrationTest {

    @MockBean
    private DataLoader dataLoader;

    @Autowired
    private ScheduledExchangeRateUpdater scheduledExchangeRateUpdater;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @MockitoSpyBean
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        exchangeRateRepository.deleteAll();
    }

    @Test
    void updateExchangeRates_ExecutesSuccessfully() {
        LocalDate testDate = LocalDate.now();
        ExchangeRate existingRate = new ExchangeRate("EUR", "USD", testDate, new BigDecimal("1.1000"));
        exchangeRateRepository.save(existingRate);

        scheduledExchangeRateUpdater.updateExchangeRates();

        verify(exchangeRateService, times(1)).syncLatestExchangeRates();
    }

    @Test
    void updateExchangeRates_HandlesException() {
        doThrow(new RuntimeException("API Error"))
                .when(exchangeRateService).syncLatestExchangeRates();

        scheduledExchangeRateUpdater.updateExchangeRates();

        verify(exchangeRateService, times(1)).syncLatestExchangeRates();
    }

    @Test
    void schedulerBean_IsCreated() {
        assertThat(scheduledExchangeRateUpdater).isNotNull();
    }

    @Test
    void exchangeRateService_IsInjected() {
        assertThat(exchangeRateService).isNotNull();
    }
}
