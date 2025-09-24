package com.kapa.binance.base.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class SwaggerConfig {

    @Value("${config.swagger.server}")
    private List<String> server;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Bean
    public OpenAPI openAPI() {
        List<Server> servers = server.stream()
                .map(url -> new Server().url(url + contextPath))
                .collect(Collectors.toList());

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .addSecurityItem(new SecurityRequirement().addList("Basic Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createBearerScheme())
                        .addSecuritySchemes("Basic Authentication", createBasicScheme())
                )
                .servers(servers);
    }

    private SecurityScheme createBearerScheme() {
        return new SecurityScheme().type(SecurityScheme.Type.HTTP).bearerFormat("JWT").scheme("bearer");
    }

    private SecurityScheme createBasicScheme() {
        return new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic");
    }

}
