package com.assignment.eaglebank.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                .info(new Info()
                        .title("Eagle Bank API")
                        .version("v1.0.0")
                        .description("Eagle Bank Rest APIs. These operations are secured with JWT authentication.\n\n" +
                                "**How to use JWT Authentication:**\n" +
                                "1. Create a user account using POST /v1/users\n" +
                                "2. Login using POST /auth/login to get a JWT token\n" +
                                "3. Click the 'Authorize' button below and enter: Bearer {your-jwt-token}\n" +
                                "4. You can now access protected endpoints"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .description("Enter JWT token in format: Bearer {token}\n\nExample: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")));
    }
} 