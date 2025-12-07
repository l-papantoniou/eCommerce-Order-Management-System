package org.lampis.gateway.util;

import org.lampis.gateway.config.JwtProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.interfaces.RSAPublicKey;
import java.util.Date;

/**
 * JWT utility for token validation
 * <p>
 * Features:
 * - Loads public key from JWKS endpoint at startup
 * - Validates JWT tokens offline using RSA public key
 * - Extracts user claims (ID, role, email)
 */
@Slf4j
@Component
public class JwtUtil {

    private final RSAPublicKey publicKey;
    private final JwtProperties jwtProperties;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        try {
            this.publicKey = loadPublicKeyFromJwks(jwtProperties.getJwksUri());
            log.info("Successfully loaded public key from JWKS endpoint: {}", jwtProperties.getJwksUri());
            log.info("Issuer configured: {}", jwtProperties.getIssuer());
        } catch (Exception e) {
            log.error("Failed to load public key", e);
            throw new RuntimeException("Cannot initialize JWT validation", e);
        }
    }

    /**
     * Load public key from JWKS endpoint
     */
    private RSAPublicKey loadPublicKeyFromJwks(String jwksUri) throws Exception {
        log.info("Fetching JWKS from: {}", jwksUri);

        // Fetch JWKS JSON from authorization server
        WebClient webClient = WebClient.create();
        String jwksJson = webClient.get()
                .uri(jwksUri)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // block since we need it at startup

        if (jwksJson == null || jwksJson.isEmpty()) {
            throw new RuntimeException("Empty JWKS response from: " + jwksUri);
        }

        log.debug("JWKS response: {}", jwksJson);

        // Parse JWKS JSON
        JWKSet jwkSet = JWKSet.parse(jwksJson);

        if (jwkSet.getKeys().isEmpty()) {
            throw new RuntimeException("No keys found in JWKS");
        }

        // Get the first RSA key
        RSAKey rsaKey = (RSAKey) jwkSet.getKeys().get(0);

        log.info("Loaded RSA key with kid: {}", rsaKey.getKeyID());

        // Convert to Java RSA public key
        return rsaKey.toRSAPublicKey();
    }

    /**
     * Validates JWT token OFFLINE using local public key
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)  // Verify with public key from JWKS
                    .requireIssuer(jwtProperties.getIssuer())   // Validate issuer
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Check expiration
            if (claims.getExpiration().before(new Date())) {
                log.warn("Token expired at: {}", claims.getExpiration());
                return false;
            }

            log.debug("Token validated successfully for user: {}", claims.getSubject());
            return true;

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("Invalid token signature: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("Malformed token: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts user ID from token
     */
    public String getUserId(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * Extract user roles from token
     */
    public String getUserRole(String token) {
        Claims claims = getClaims(token);
        if (claims == null) return null;

        // Try different claim names
        Object role = claims.get("role");
        if (role == null) role = claims.get("roles");
        if (role == null) role = claims.get("authorities");

        return role != null ? role.toString() : null;
    }

    /**
     * Extracts email from token
     */
    public String getUserEmail(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.get("email", String.class) : null;
    }

    /**
     * Get all claims from token
     */
    private Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("Cannot parse token claims: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        Claims claims = getClaims(token);
        return claims == null || claims.getExpiration().before(new Date());
    }
}