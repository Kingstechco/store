package com.securitease.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

/**
 * OpenAPI configuration for the Store application.
 *
 * <p>This configuration sets up Swagger/OpenAPI documentation for all REST endpoints, providing interactive API
 * documentation and testing capabilities. The configuration includes application metadata, server information, and API
 * versioning details.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI storeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Store API")
                        .description(
                                "Enterprise-grade Spring Boot application for managing customers, orders, and products with comprehensive REST APIs")
                        .version("v2.0")
                        .contact(new Contact().name("Store Application Team").email("support@store-app.com"))
                        .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Development server"),
                        new Server().url("https://api.store-app.com").description("Production server")));
    }
}
