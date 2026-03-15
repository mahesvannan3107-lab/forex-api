package com.crewmeister.forex.service;

import com.crewmeister.forex.dto.CurrencyDto;
import com.crewmeister.forex.mapper.CurrencyMapper;
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
public class CurrencyService implements ICurrencyService {

    private final CurrencyRepository currencyRepository;
    private final CurrencyMapper currencyMapper;

    @Transactional(readOnly = true)
    @Override
    public List<CurrencyDto> getAllCurrencies() {
        log.debug("Fetching all currencies");
        return currencyRepository.findAll().stream()
                .map(currencyMapper::toDto)
                .collect(Collectors.toList());
    }
}

