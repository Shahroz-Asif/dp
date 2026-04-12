package com.example.recipemaker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI recipeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Patient Recipe System API")
                        .description("REST API for composing patient recipes with modifiable/non-modifiable components, " +
                                "compatibility checking against patient conditions, and searching using the Composite Strategy pattern.")
                        .version("1.0.0"));
    }
}
