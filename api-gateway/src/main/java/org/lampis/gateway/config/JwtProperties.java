package org.lampis.gateway.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * JWT configuration properties
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWKS endpoint URL from authorization server
     */
    @NotBlank(message = "JWT JWKS URI must be configured")
    private String jwksUri;

    /**
     * Expected issuer of JWT tokens
     */
    @NotBlank(message = "JWT issuer must be configured")
    private String issuer;
}