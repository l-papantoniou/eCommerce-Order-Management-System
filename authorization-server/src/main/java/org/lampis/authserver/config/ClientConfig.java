package org.lampis.authserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.UUID;

/**
 * OAuth2 Client Configuration
 * <p>
 * Registers OAuth2 clients (applications) that can request tokens from this authorization server.
 * <p>
 * Registered Clients:
 * 1. ecommerce-api-gateway - For API Gateway authentication (client credentials)
 * 2. ecommerce-web-app - For web application (authorization code flow)
 * 3. ecommerce-mobile-app - For mobile apps (authorization code with PKCE)
 * 4. ecommerce-admin - For admin portal (password grant - demo only)
 * <p>
 * Current Implementation: In-Memory Storage
 * Production Alternative: Database Storage (JdbcRegisteredClientRepository)
 */
@Configuration
public class ClientConfig {

    private final PasswordEncoder passwordEncoder;

    public ClientConfig(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registered Client Repository
     * <p>
     * Defines OAuth2 clients that can request tokens from this authorization server.
     * Each client has specific grant types, scopes, and token settings.
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {

        // Client 1: API Gateway - For service-to-service communication
        RegisteredClient apiGatewayClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("ecommerce-api-gateway")
                .clientSecret(passwordEncoder.encode("gateway-secret-2024"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("api.read")
                .scope("api.write")
                .scope("service.access")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .build())
                .build();

        // Client 2: Web Application - For user authentication via browser
        RegisteredClient webAppClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("ecommerce-web-app")
                .clientSecret(passwordEncoder.encode("web-secret-2024"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:3000/callback")
                .redirectUri("http://localhost:3000/authorized")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope(OidcScopes.EMAIL)
                .scope("orders.read")
                .scope("orders.write")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(30))
                        .refreshTokenTimeToLive(Duration.ofDays(30))
                        .reuseRefreshTokens(false)
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .requireProofKey(false)
                        .build())
                .build();

        // Client 3: Mobile App - With PKCE for enhanced security
        RegisteredClient mobileAppClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("ecommerce-mobile-app")
                // No client secret for public clients
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("com.ecommerce.mobile://callback")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("orders.read")
                .scope("orders.write")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(15))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .reuseRefreshTokens(false)
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(true) // PKCE required for public clients
                        .build())
                .build();


        RegisteredClient adminClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("ecommerce-admin")
                .clientSecret(passwordEncoder.encode("admin-secret-2024"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:8080/admin/callback")
                .scope("admin.read")
                .scope("admin.write")
                .scope("user.manage")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(30))
                        .refreshTokenTimeToLive(Duration.ofHours(8))
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(
                apiGatewayClient,
                webAppClient,
                mobileAppClient,
                adminClient
        );
    }
}