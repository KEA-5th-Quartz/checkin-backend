package com.quartz.checkin.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CheckIn API",
                description = "CheckIn 서비스 API 문서",
                version = "1.0.0"
        )
)
public class SwaggerConfig {

    @Value("${springdoc.baseurl}")
    private String baseUrl;

    @Bean
    public OpenAPI customOpenAPI() {

        SecurityScheme apiKey = new SecurityScheme()
                .type(Type.APIKEY)
                .in(In.HEADER)
                .name("Authorization");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("Bearer Token");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes("Bearer Token", apiKey))
                .addSecurityItem(securityRequirement)
                .servers(List.of(new Server().url(baseUrl)))
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("CheckIn API Docs")
                );
    }
}
