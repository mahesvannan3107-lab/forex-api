package com.crewmeister.forex.service;

import com.crewmeister.forex.entity.Currency;
import com.crewmeister.forex.dto.CurrencyDto;
import com.crewmeister.forex.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRepository currencyRepository;

    @Transactional(readOnly = true)
    public List<CurrencyDto> getAllCurrencies() {
        log.debug("Fetching all currencies");
        return currencyRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private CurrencyDto toDto(Currency currency) {
        return new CurrencyDto(currency.getCode(), currency.getName());
    }
}

