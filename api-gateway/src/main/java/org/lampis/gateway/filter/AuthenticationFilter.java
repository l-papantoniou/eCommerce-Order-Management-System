package org.lampis.gateway.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.lampis.gateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();

            String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // Check if header exists
            if (authorization == null || authorization.isEmpty()) {
                log.warn("Missing Authorization header for path: {}", path);
                return onError(exchange, "Missing authorization header", HttpStatus.UNAUTHORIZED);
            }

            // Check if header has Bearer prefix
            if (!authorization.startsWith(BEARER_PREFIX)) {
                log.warn("Invalid Authorization header format for path: {}", path);
                return onError(exchange, "Invalid authorization format. Expected: Bearer <token>", HttpStatus.UNAUTHORIZED);
            }

            // Extract token
            String token = authorization.substring(BEARER_PREFIX.length()).trim();

            // Check if token is empty after extraction
            if (token.isEmpty()) {
                log.warn("Empty token for path: {}", path);
                return onError(exchange, "Empty authorization token", HttpStatus.UNAUTHORIZED);
            }

            // Validate token
            if (!jwtUtil.validateToken(token)) {
                log.warn("Invalid or expired token for path: {}", path);
                return onError(exchange, "Invalid or expired token", HttpStatus.FORBIDDEN);
            }

            // Extract user information from token
            String userId = jwtUtil.getUserId(token);
            String userRole = jwtUtil.getUserRole(token);
            String userEmail = jwtUtil.getUserEmail(token);

            // Validate extracted claims
            if (userId == null || userId.isEmpty()) {
                log.warn("Could not extract user ID from token for path: {}", path);
                return onError(exchange, "Invalid token claims", HttpStatus.FORBIDDEN);
            }

            // Build modified request with user info headers
            ServerHttpRequest.Builder requestBuilder = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-Authenticated", "true");

            // Add optional headers only if present
            if (userRole != null && !userRole.isEmpty()) {
                requestBuilder.header("X-User-Role", userRole);
            }
            if (userEmail != null && !userEmail.isEmpty()) {
                requestBuilder.header("X-User-Email", userEmail);
            }

            ServerHttpRequest modifiedRequest = requestBuilder.build();

            log.debug("Authenticated user: {} (role: {}) for path: {}", userId, userRole, path);

            // Continue with modified request
            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        });
    }


    /**
     * Returns error response with JSON body
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-type", "application/json");

        String errorResponse = String.format(
                "{\"error\":\"%s\",\"status\":%d,\"timestamp\":\"%s\"}",
                message,
                status.value(),
                java.time.Instant.now().toString()
        );

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(errorResponse.getBytes()))
        );
    }

    /**
     * Configuration class for AuthenticationFilter
     */
    @Getter
    @Setter
    public static class Config {
    }
}
