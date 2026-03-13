package com.crewmeister.forex.repository;

import com.crewmeister.forex.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    // Find all by source currency ordered by date descending
    List<ExchangeRate> findBySourceCurrencyOrderByDateDesc(String sourceCurrency);

    // Find by source currency and date (all target currencies)
    List<ExchangeRate> findBySourceCurrencyAndDate(String sourceCurrency, LocalDate date);

    // Find latest rates for all target currencies from a source currency
    @Query("SELECT e FROM ExchangeRate e WHERE e.sourceCurrency = :sourceCurrency " +
            "AND e.date = (SELECT MAX(e2.date) FROM ExchangeRate e2 " +
            "WHERE e2.sourceCurrency = e.sourceCurrency AND e2.targetCurrency = e.targetCurrency)")
    List<ExchangeRate> findLatestRatesBySourceCurrency(@Param("sourceCurrency") String sourceCurrency);

    // Find by source and target currency ordered by date descending
    List<ExchangeRate> findBySourceCurrencyAndTargetCurrencyOrderByDateDesc(
            String sourceCurrency, String targetCurrency);

    // Find latest rate by source and target (first result ordered by date desc)
    default Optional<ExchangeRate> findLatestRateBySourceAndTarget(String sourceCurrency, String targetCurrency) {
        List<ExchangeRate> rates = findBySourceCurrencyAndTargetCurrencyOrderByDateDesc(sourceCurrency, targetCurrency);
        return rates.isEmpty() ? Optional.empty() : Optional.of(rates.get(0));
    }

    // Find by source, target, and date
    Optional<ExchangeRate> findBySourceCurrencyAndTargetCurrencyAndDate(
            String sourceCurrency, String targetCurrency, LocalDate date);
}

