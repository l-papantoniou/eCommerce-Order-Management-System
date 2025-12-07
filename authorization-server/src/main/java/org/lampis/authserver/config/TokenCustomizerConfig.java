package org.lampis.authserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.stream.Collectors;

/**
 * Token Customizer Configuration
 * <p>
 * Customizes JWT tokens by adding additional claims.
 * These claims can be read by downstream services (like API Gateway)
 * to implement authorization logic.
 */
@Configuration
public class TokenCustomizerConfig {

    /**
     * JWT Token Customizer
     * <p>
     * Adds custom claims to JWT tokens:
     * - authorities: User roles and permissions
     * - username: Username/email
     * - user_id: User identifier
     * - timestamp: Token generation time
     * <p>
     * These claims are available to:
     * - API Gateway for authorization decisions
     * - Downstream microservices for user context
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {

            // Get the authenticated user/client
            Authentication principal = context.getPrincipal();

            if (principal != null && principal.getAuthorities() != null) {

                // Extract authorities (roles and permissions)
                var authorities = principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());

                // Add custom claims to JWT
                context.getClaims().claim("authorities", authorities);
                context.getClaims().claim("username", principal.getName());

                // Add user_id (extract from username if email format)
                String username = principal.getName();
                String userId = extractUserId(username);
                context.getClaims().claim("user_id", userId);

                // Add timestamp for debugging
                context.getClaims().claim("timestamp", System.currentTimeMillis());

                // Add email if available
                if (username.contains("@")) {
                    context.getClaims().claim("email", username);
                }
            }
        };
    }

    /**
     * Extract user ID from username
     * For demo: uses email prefix as user ID
     * Production: should fetch from user database
     */
    private String extractUserId(String username) {
        if (username.contains("@")) {
            return username.substring(0, username.indexOf("@"));
        }
        return username;
    }
}