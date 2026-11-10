package com.bloodbridge.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Interactive API documentation at /swagger-ui.html (log in via /api/auth/login, then Authorize with the token). */
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI bloodBridgeApi() {
        return new OpenAPI()
                .info(new Info().title("BloodBridge API").version("1.0.0")
                        .description("Blood donation coordination prototype. Synthetic data only; not a medical system."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
