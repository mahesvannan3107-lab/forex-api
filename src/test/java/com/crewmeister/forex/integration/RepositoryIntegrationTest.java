package com.crewmeister.forex.integration;

import com.crewmeister.forex.entity.Currency;
import com.crewmeister.forex.entity.ExchangeRate;
import com.crewmeister.forex.repository.CurrencyRepository;
import com.crewmeister.forex.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.sql.init.mode=never"
})
class RepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2026, 3, 1);
    }

    @Test
    void currencyRepository_SaveAndFind() {
        Currency currency = new Currency();
        currency.setCode("EUR");
        currency.setName("Euro");

        Currency saved = currencyRepository.save(currency);

        assertThat(saved.getCode()).isEqualTo("EUR");
        assertThat(saved.getName()).isEqualTo("Euro");
        assertThat(saved.getId()).isNotNull();

        List<Currency> allCurrencies = currencyRepository.findAll();
        assertThat(allCurrencies).hasSize(1);
        assertThat(allCurrencies.get(0).getCode()).isEqualTo("EUR");
        assertThat(allCurrencies.get(0).getName()).isEqualTo("Euro");
    }

    @Test
    void currencyRepository_FindAll() {
        Currency eur = new Currency();
        eur.setCode("EUR");
        eur.setName("Euro");
        currencyRepository.save(eur);

        Currency usd = new Currency();
        usd.setCode("USD");
        usd.setName("US Dollar");
        currencyRepository.save(usd);

        List<Currency> currencies = currencyRepository.findAll();

        assertThat(currencies).hasSize(2);
        assertThat(currencies).extracting(Currency::getCode).containsExactlyInAnyOrder("EUR", "USD");
    }

    @Test
    void exchangeRateRepository_SaveAndFind() {
        ExchangeRate rate = new ExchangeRate("EUR", "USD", testDate, new BigDecimal("1.1641"));
        ExchangeRate saved = exchangeRateRepository.save(rate);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSourceCurrency()).isEqualTo("EUR");
        assertThat(saved.getTargetCurrency()).isEqualTo("USD");
        assertThat(saved.getRate()).isEqualByComparingTo("1.1641");
    }

    @Test
    void exchangeRateRepository_FindBySourceCurrencyAndTargetCurrencyAndDate() {
        ExchangeRate rate = new ExchangeRate("EUR", "USD", testDate, new BigDecimal("1.1641"));
        exchangeRateRepository.save(rate);

        Optional<ExchangeRate> found = exchangeRateRepository
                .findBySourceCurrencyAndTargetCurrencyAndDate("EUR", "USD", testDate);

        assertThat(found).isPresent();
        assertThat(found.get().getRate()).isEqualByComparingTo("1.1641");
    }

    @Test
    void exchangeRateRepository_FindLatestRateBySourceAndTarget() {
        ExchangeRate oldRate = new ExchangeRate("EUR", "USD", testDate.minusDays(5), new BigDecimal("1.1500"));
        ExchangeRate newRate = new ExchangeRate("EUR", "USD", testDate, new BigDecimal("1.1641"));
        
        exchangeRateRepository.save(oldRate);
        exchangeRateRepository.save(newRate);

        Optional<ExchangeRate> latest = exchangeRateRepository
                .findLatestRateBySourceAndTarget("EUR", "USD");

        assertThat(latest).isPresent();
        assertThat(latest.get().getDate()).isEqualTo(testDate);
        assertThat(latest.get().getRate()).isEqualByComparingTo("1.1641");
    }

    @Test
    void exchangeRateRepository_FindLatestRatesBySourceCurrency() {
        ExchangeRate eurUsd = new ExchangeRate("EUR", "USD", testDate, new BigDecimal("1.1641"));
        ExchangeRate eurGbp = new ExchangeRate("EUR", "GBP", testDate, new BigDecimal("0.8590"));
        ExchangeRate usdEur = new ExchangeRate("USD", "EUR", testDate, new BigDecimal("0.8590"));

        exchangeRateRepository.save(eurUsd);
        exchangeRateRepository.save(eurGbp);
        exchangeRateRepository.save(usdEur);

        List<ExchangeRate> rates = exchangeRateRepository.findLatestRatesBySourceCurrency("EUR");

        assertThat(rates).hasSize(2);
        assertThat(rates).extracting(ExchangeRate::getTargetCurrency)
                .containsExactlyInAnyOrder("USD", "GBP");
    }

    @Test
    void exchangeRateRepository_FindBySourceCurrencyAndDate() {
        ExchangeRate eurUsd = new ExchangeRate("EUR", "USD", testDate, new BigDecimal("1.1641"));
        ExchangeRate eurGbp = new ExchangeRate("EUR", "GBP", testDate, new BigDecimal("0.8590"));

        exchangeRateRepository.save(eurUsd);
        exchangeRateRepository.save(eurGbp);

        List<ExchangeRate> rates = exchangeRateRepository
                .findBySourceCurrencyAndDate("EUR", testDate);

        assertThat(rates).hasSize(2);
        assertThat(rates).extracting(ExchangeRate::getTargetCurrency)
                .containsExactlyInAnyOrder("USD", "GBP");
    }

    @Test
    void exchangeRateRepository_FindBySourceCurrencyAndTargetCurrencyOrderByDateDesc() {
        LocalDate date1 = testDate.minusDays(2);
        LocalDate date2 = testDate.minusDays(1);
        LocalDate date3 = testDate;

        ExchangeRate rate1 = new ExchangeRate("EUR", "USD", date1, new BigDecimal("1.1500"));
        ExchangeRate rate2 = new ExchangeRate("EUR", "USD", date2, new BigDecimal("1.1600"));
        ExchangeRate rate3 = new ExchangeRate("EUR", "USD", date3, new BigDecimal("1.1641"));

        exchangeRateRepository.save(rate1);
        exchangeRateRepository.save(rate2);
        exchangeRateRepository.save(rate3);

        List<ExchangeRate> rates = exchangeRateRepository
                .findBySourceCurrencyAndTargetCurrencyOrderByDateDesc("EUR", "USD");

        assertThat(rates).hasSize(3);
        assertThat(rates.get(0).getDate()).isEqualTo(date3);
        assertThat(rates.get(1).getDate()).isEqualTo(date2);
        assertThat(rates.get(2).getDate()).isEqualTo(date1);
    }

    @Test
    void exchangeRateRepository_FindBySourceCurrencyOrderByDateDesc() {
        ExchangeRate eurUsd = new ExchangeRate("EUR", "USD", testDate, new BigDecimal("1.1641"));
        ExchangeRate eurGbp = new ExchangeRate("EUR", "GBP", testDate.minusDays(1), new BigDecimal("0.8590"));
        ExchangeRate usdEur = new ExchangeRate("USD", "EUR", testDate, new BigDecimal("0.8590"));

        exchangeRateRepository.save(eurUsd);
        exchangeRateRepository.save(eurGbp);
        exchangeRateRepository.save(usdEur);

        List<ExchangeRate> rates = exchangeRateRepository.findBySourceCurrencyOrderByDateDesc("EUR");

        assertThat(rates).hasSize(2);
        assertThat(rates).allMatch(rate -> rate.getSourceCurrency().equals("EUR"));
        assertThat(rates.get(0).getDate()).isAfter(rates.get(1).getDate());
    }

    @Test
    void exchangeRateRepository_UpdateExistingRate() {
        ExchangeRate rate = new ExchangeRate("EUR", "USD", testDate, new BigDecimal("1.1641"));
        ExchangeRate saved = exchangeRateRepository.save(rate);

        saved.setRate(new BigDecimal("1.1700"));
        saved.setUpdatedBy("TEST");
        ExchangeRate updated = exchangeRateRepository.save(saved);

        assertThat(updated.getRate()).isEqualByComparingTo("1.1700");
        assertThat(updated.getUpdatedBy()).isEqualTo("TEST");
    }

    @Test
    void exchangeRateRepository_DeleteAll() {
        ExchangeRate rate1 = new ExchangeRate("EUR", "USD", testDate, new BigDecimal("1.1641"));
        ExchangeRate rate2 = new ExchangeRate("EUR", "GBP", testDate, new BigDecimal("0.8590"));

        exchangeRateRepository.save(rate1);
        exchangeRateRepository.save(rate2);

        exchangeRateRepository.deleteAll();

        List<ExchangeRate> rates = exchangeRateRepository.findAll();
        assertThat(rates).isEmpty();
    }

    @Test
    void exchangeRateRepository_CountRecords() {
        ExchangeRate rate1 = new ExchangeRate("EUR", "USD", testDate, new BigDecimal("1.1641"));
        ExchangeRate rate2 = new ExchangeRate("EUR", "GBP", testDate, new BigDecimal("0.8590"));

        exchangeRateRepository.save(rate1);
        exchangeRateRepository.save(rate2);

        long count = exchangeRateRepository.count();
        assertThat(count).isEqualTo(2);
    }
}
