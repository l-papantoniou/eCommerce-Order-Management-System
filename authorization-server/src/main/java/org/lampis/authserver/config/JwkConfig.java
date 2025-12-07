package org.lampis.authserver.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * JSON Web Key (JWK) Configuration
 * <p>
 * Generates RSA key pair for signing JWT tokens.
 * The public key is exposed via /oauth2/jwks endpoint for token validation.
 * <p>
 * Security Notes:
 * - Current Implementation: Generates new key pair on startup (in-memory)
 * - Production: Use persistent keys (file/database) to prevent invalidating tokens on restart
 * - Key Rotation: Implement key rotation strategy for enhanced security
 */
@Configuration
public class JwkConfig {

    /**
     * JWK Source Bean
     * <p>
     * Provides the JSON Web Key Set (JWKS) containing the public key(s)
     * used to verify JWT token signatures.
     * <p>
     * The public key is exposed at: GET /oauth2/jwks
     * API Gateway fetches this to validate tokens offline.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();

        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * Generate RSA Key Pair
     * <p>
     * Creates a 2048-bit RSA key pair for JWT signing.
     * <p>
     * Algorithm: RSA
     * Key Size: 2048 bits (industry standard)
     * Usage: Signing JWT tokens (RS256 algorithm)
     */
    private KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA key pair", e);
        }
    }

    /**
     * JWT Decoder Bean
     * <p>
     * Decodes and validates JWT tokens using the public key from JWK source.
     * Used by the authorization server itself when acting as a resource server.
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }
}