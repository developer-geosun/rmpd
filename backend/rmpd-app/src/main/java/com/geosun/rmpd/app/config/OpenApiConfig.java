package com.geosun.rmpd.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rmpdOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("RMPD API")
                        .description("REST API системи реєстрації RMPD100")
                        .version("0.1.0")
                        .contact(new Contact().name("GeoSun")));
    }
}
