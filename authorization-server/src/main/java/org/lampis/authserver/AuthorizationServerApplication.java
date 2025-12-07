package org.lampis.authserver;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Authorization Server Application
 * <p>
 * OAuth 2.0 Authorization Server with OpenID Connect (OIDC) support.
 * <p>
 * Features:
 * - Multiple grant types (client_credentials, authorization_code, password, refresh_token)
 * - JWT token generation with RSA signing
 * - JWKS endpoint for public key distribution
 * - Token introspection and revocation
 * - User authentication with in-memory store
 * - Customizable token lifetime
 * <p>
 * Endpoints:
 * - Token: POST /oauth2/token
 * - JWKS: GET /oauth2/jwks
 * - Authorization: GET /oauth2/authorize
 * - Introspect: POST /oauth2/introspect
 * - Revoke: POST /oauth2/revoke
 * - UserInfo: GET /userinfo
 * - Discovery: GET /.well-known/oauth-authorization-server
 */
@SpringBootApplication
public class AuthorizationServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthorizationServerApplication.class, args);
    }
}