package com.cobre.notification.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Notification Service API",
                version = "1.0",
                description = "Self-service REST API for querying, inspecting, and replaying " +
                              "notification events delivered to client webhook endpoints."
        ),
        security = @SecurityRequirement(name = "BearerAuth")
)
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT access token issued by the OAuth2 authorization server. " +
                      "Must contain the 'clientId' claim and the appropriate scope " +
                      "('notification:read' or 'notification:write')."
)
public class OpenApiConfig {
}
