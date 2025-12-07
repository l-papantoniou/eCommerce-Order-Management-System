package org.lampis.authserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * Security configuration for OAuth2 Authorization Server
 * <p>
 * Configures two security filter chains:
 * 1. OAuth2 Authorization Server endpoints (token, authorize, jwks, etc.)
 * 2. Default security for other endpoints (login, logout, custom endpoints)
 */
@Configuration
public class SecurityConfig {

    /**
     * OAuth2 Authorization Server Security Filter Chain
     * <p>
     * Handles all OAuth2 and OIDC endpoints:
     * - /oauth2/authorize       - Authorization endpoint
     * - /oauth2/token          - Token endpoint
     * - /oauth2/jwks           - JSON Web Key Set endpoint
     * - /oauth2/introspect     - Token introspection
     * - /oauth2/revoke         - Token revocation
     * - /userinfo              - UserInfo endpoint (OIDC)
     * - /.well-known/oauth-authorization-server - Discovery endpoint
     * - /.well-known/openid-configuration - OpenID configuration
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {

        // Apply OAuth 2.0 default security configuration
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        // Enable OpenID Connect 1.0
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());

        // Redirect unauthenticated users to login page for authorization endpoint
        http.exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                ));

        // Accept JWT tokens for resource server endpoints
        http.oauth2ResourceServer(resourceServer -> resourceServer
                .jwt(Customizer.withDefaults())
        );

        return http.build();
    }

    /**
     * Default Security Filter Chain
     * <p>
     * Handles everything NOT handled by the OAuth2 filter chain:
     * - /login         - Form login
     * - /logout        - Logout
     * - Custom API endpoints
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints
                        .requestMatchers(
                                "/actuator/health",
                                "/.well-known/**"
                        ).permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                // Enable form login
                .formLogin(Customizer.withDefaults());

        return http.build();
    }

    /**
     * Authorization Server Settings
     * <p>
     * Configures the OAuth2 authorization server settings.
     * Uses default settings which include:
     * - Issuer: http://localhost:9000 (auto-detected from request)
     * - Authorization endpoint: /oauth2/authorize
     * - Token endpoint: /oauth2/token
     * - JWK Set endpoint: /oauth2/jwks
     * - Token introspection endpoint: /oauth2/introspect
     * - Token revocation endpoint: /oauth2/revoke
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .build();
    }
}