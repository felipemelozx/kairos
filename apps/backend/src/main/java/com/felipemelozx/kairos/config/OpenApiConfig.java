package com.felipemelozx.kairos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI kairosOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kairos API")
                        .description("Time-centered productivity system API")
                        .version("1.0.0"))
                .schemaRequirement("cookieAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("ACCESS_TOKEN"));
    }
}
