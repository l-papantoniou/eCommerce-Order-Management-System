package org.lampis.gateway.filter;


import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.lampis.gateway.config.RateLimitProperties;
import org.lampis.gateway.service.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Rate limiting filter using Token Bucket algorithm
 * <p>
 * Can rate limit by:
 * - User ID (authenticated users)
 * - IP address (anonymous users)
 * - API key
 * - Global (all requests)
 */
@Slf4j
@Component
public class RateLimitingFilter extends AbstractGatewayFilterFactory<RateLimitingFilter.Config> {


    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private RateLimitProperties rateLimitProperties;

    public RateLimitingFilter() {
        super(Config.class);
    }


    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {

            // Step 1: Check if rate limiting is enabled globally
            if (!rateLimitProperties.isEnabled()) {
                log.debug("Rate limiting is disabled, skipping");
                return chain.filter(exchange);
            }

            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // Step 2: Determine rate limit key (who to limit)
            String key = getRateLimitKey(exchange, config);
            log.debug("Rate limit key: {} for path: {}", key, path);

            // Step 3: Get rate limit configuration (route specific or default)
            int capacity = config.getCapacity() > 0 ?
                    config.getCapacity() : rateLimitProperties.getDefaultCapacity();
            int refillTokens = config.refillTokens > 0 ?
                    config.getRefillTokens() : rateLimitProperties.getDefaultRefillTokens();
            Duration refillDuration = config.getRefillDuration() > 0 ?
                    Duration.ofSeconds(config.getRefillDuration()) :
                    Duration.ofSeconds(rateLimitProperties.getDefaultRefillDuration());

            log.debug("Rate limit config - Capacity: {}, Refill: {}/{}",
                    capacity, refillTokens, refillDuration.getSeconds());

            // Step 4: Check rate limit (try to consume 1 token)
            boolean allowed = rateLimiterService.isAllowed(key, capacity, refillTokens, refillDuration);

            if (!allowed) {
                // Rate limit exceeded - return 429 error
                log.warn("Rate limit exceeded for key: {} on path : {}", key, path);
                return onRateLimitExceeded(exchange, key, capacity, refillDuration);
            }

            // Step 5: Get remaining tokens for response headers
            long remainingTokens = rateLimiterService.getRemainingTokens(
                    key, capacity, refillTokens, refillDuration);

            // Step 6: Add rate limit info headers to response
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().add("X-RateLimit-Limit", String.valueOf(capacity));
            response.getHeaders().add("X-RateLimit-Remaining", String.valueOf(remainingTokens));
            response.getHeaders().add("X-RateLimit-Reset",
                    String.valueOf(System.currentTimeMillis() + refillDuration.toMillis()));

            log.debug("✅ Rate limit passed - Key: {}, Remaining: {}/{}",
                    key, remainingTokens, capacity);

            // Step 7: Continue to next filter
            return chain.filter(exchange);

        });
    }


    /**
     * Determine rate limit key based on strategy
     */
    private String getRateLimitKey(ServerWebExchange exchange, Config config) {
        ServerHttpRequest request = exchange.getRequest();

        switch (config.getKeyStrategy()) {
            case USER:
                // Rate limit per authenticated user
                // Gateway adds X-User-Id header after JWT validation
                String userId = request.getHeaders().getFirst("X-User-Id");
                if (userId != null && !userId.isEmpty()) {
                    return "user:" + userId;
                }
                // Fall back to IP if user not authenticated
                return "ip:" + getIpAddress(request);

            case IP:
                // Rate limit per IP address
                return "ip:" + getIpAddress(request);

            case API_KEY:
                // Rate limit per API key
                String apiKey = request.getHeaders().getFirst("X-API-Key");
                if (apiKey != null && !apiKey.isEmpty()) {
                    return "api-key:" + apiKey;
                }
                // Fall back to IP if no API key
                return "ip:" + getIpAddress(request);

            case GLOBAL:
                // Global rate limit (all requests share same bucket)
                return "global";

            default:
                // Default to IP-based
                return "ip:" + getIpAddress(request);
        }
    }

    /**
     * Extract IP address from request
     * Handles X-Forwarded-For header (if behind proxy/load balancer)
     */
    private String getIpAddress(ServerHttpRequest request) {
        // Check X-Forwarded-For header (added by proxies/load balancers)
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return forwardedFor.split(",")[0].trim();
        }

        // Fall back to remote address
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }


    private Mono<Void> onRateLimitExceeded(
            ServerWebExchange exchange,
            String key,
            int capacity,
            Duration refillDuration) {

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Add standard rate limit headers
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(capacity));
        response.getHeaders().add("X-RateLimit-Remaining", "0");
        response.getHeaders().add("X-RateLimit-Reset",
                String.valueOf(System.currentTimeMillis() + refillDuration.toMillis()));


        // Add Retry-After header (tells client when to retry)
        response.getHeaders().add("Retry-After", String.valueOf(refillDuration.getSeconds()));

        // Create JSON error response
        String errorResponse = String.format(
                "{\"error\":\"Rate limit exceeded\"," +
                        "\"message\":\"Too many requests. Please try again in %d seconds.\"," +
                        "\"status\":429," +
                        "\"timestamp\":\"%s\"}",
                refillDuration.getSeconds(),
                java.time.Instant.now().toString()
        );

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(errorResponse.getBytes()))
        );

    }


    /**
     * Configuration class for rate limiting filter
     */
    @Getter
    @Setter
    public static class Config {
        /**
         * Rate limit key strategy (how to identify users)
         */
        private KeyStrategy keyStrategy = KeyStrategy.USER;

        /**
         * Bucket capacity (max tokens)
         * 0 = use default from properties
         */
        private int capacity = 0;

        /**
         * Refill tokens
         * 0 = use default from properties
         */
        private int refillTokens = 0;

        /**
         * Refill duration in seconds
         * 0 = use default from properties
         */
        private int refillDuration = 0;
    }

    /**
     * Rate limit key strategies
     */
    public enum KeyStrategy {
        USER,      // Per authenticated user (from X-User-Id header)
        IP,        // Per IP address
        API_KEY,   // Per API key (from X-API-Key header)
        GLOBAL     // Global (all users share same bucket)
    }
}
