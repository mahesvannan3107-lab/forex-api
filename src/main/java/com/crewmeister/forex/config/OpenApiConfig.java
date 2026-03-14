package com.crewmeister.forex.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.url}")
    private String serverUrl;

    @Bean
    public OpenAPI forexOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl(serverUrl);
        localServer.setDescription("Local Development Server");

        Contact contact = new Contact();
        contact.setName("Forex API Support");
        contact.setEmail("support@forex-api.com");

        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("Foreign Exchange Rate API")
                .version("1.0.0")
                .description("REST API for fetching foreign exchange rates from Bundesbank. " +
                        "Provides endpoints for retrieving exchange rates, currency conversion, and historical data.")
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}
