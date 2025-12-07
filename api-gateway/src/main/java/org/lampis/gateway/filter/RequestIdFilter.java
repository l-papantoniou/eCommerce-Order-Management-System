package org.lampis.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global filter that adds a unique request ID to every request
 * <p>
 * The request id is used for:
 * - Distributed tracing across microservices
 * - Correlating logs
 * - Debugging and support
 * <p>
 * If a request already has an X-Request-Id header, it will be preserved
 * Otherwise a new UUID is generated
 */
@Slf4j
@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Check if request already has a request ID (from client or upstream proxy)
        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);

        if (requestId == null || requestId.isEmpty()) {
            requestId = generateRequestId();
            log.debug("Generated new request ID: {}", requestId);
        } else {
            log.debug("Using existing request ID: {}", requestId);
        }

        // Store request ID for use in other filters
        final String finalRequestId = requestId;

        // Add request ID to request headers (for downstream services)
        ServerHttpRequest modifiedRequest = request.mutate()
                .header(REQUEST_ID_HEADER, requestId)
                .build();

        // Add request headers to response headers for client visibility
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

        log.info("[{}] {} {}", requestId, request.getMethod(), request.getPath());

        // Continue filter chain with modified request
        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .doFinally(signalType -> {
                    log.debug("[{}] Request completed with signal: {}", finalRequestId, signalType);
                });
    }


    /**
     * Generates a unique request id
     *
     * @return UUID String
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Set highest precedence so this filter executes first
     * This ensures all other filters and downstream services have access to the request ID
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
