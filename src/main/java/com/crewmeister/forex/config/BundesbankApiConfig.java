package com.crewmeister.forex.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "bundesbank.api")
@Data
public class BundesbankApiConfig {

    private String baseUrl;
    private String format;
    private String language;
    private int defaultObservations;
    private Endpoint endpoint;

    @Data
    public static class Endpoint {
        private String allCurrencies;
    }
}
