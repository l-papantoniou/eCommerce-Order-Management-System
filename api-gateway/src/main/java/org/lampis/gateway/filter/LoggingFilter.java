package org.lampis.gateway.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;


@Slf4j
@Component
public class LoggingFilter extends AbstractGatewayFilterFactory<LoggingFilter.Config> {


    public LoggingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // ============ PRE-PROCESSING (Before Request) ==============
            long startTime = Instant.now().toEpochMilli();
            ServerHttpRequest request = exchange.getRequest();

            String method = request.getMethod().name();
            String path = request.getPath().value();
            String requestId = request.getHeaders().getFirst("X-Request-Id");
            String prefix = config.getLogPrefix() != null ? config.getLogPrefix() + " " : "";

            log.info("Request: {} {}", method, path);

            // ========== POST-PROCESSING (After Response) ==========
            return chain.filter(exchange)
                    .then(Mono.fromRunnable(() -> {
                        // ========== POST-PROCESSING (After Response) ==========
                        long endTime = Instant.now().toEpochMilli();
                        long duration = endTime - startTime;

                        ServerHttpResponse response = exchange.getResponse();
                        Integer statusCode = exchange.getResponse().getStatusCode() != null
                                ? exchange.getResponse().getStatusCode().value()
                                : 0;

                        // Log slow requests as warnings
                        if (config.getSlowRequestThreshold() > 0
                                && duration > config.getSlowRequestThreshold()) {
                            log.warn("{}SLOW REQUEST <== [{}] {} {} - Status: {} - Duration: {}ms",
                                    prefix, requestId, method, path, statusCode, duration);
                        }

                        // Log with headers if configured
                        else if (config.isIncludeHeaders()) {
                            HttpHeaders headers = filterHeaders(response.getHeaders(), config.getExcludedHeaders());
                            log.info("{}<=== [{}] {} {} - Status: {} - Duration: {}ms - Headers: {}",
                                    prefix, requestId, method, path, statusCode, duration, headers);
                        }
                        // Standard log
                        else {
                            log.info("{}<== [{}] {} {} - Status: {} - Duration: {}ms",
                                    prefix, requestId, method, path, statusCode, duration);
                        }
                    }));
        };
    }


    /**
     * Filters out sensitive headers from logging
     */
    private HttpHeaders filterHeaders(HttpHeaders headers, Set<String> excludedHeaders) {
        HttpHeaders filtered = new HttpHeaders();
        headers.forEach((key, value) -> {
            if (!excludedHeaders.contains(key.toLowerCase())) {
                filtered.addAll(key, value);
            }
        });
        return filtered;
    }

    @Getter
    @Setter
    public static class Config {
        /**
         * Custom prefix to add to log messages (e.g., "[ORDER-SVC]")
         */
        private String logPrefix;

        /**
         * Whether to include request/response headers in logs
         * Default: false
         */
        private boolean includeHeaders = false;

        /**
         * Threshold in milliseconds for slow request warnings
         * If request duration exceeds this, log as warning
         * Set to 0 to disable
         * Default: 0 (disabled)
         */
        private long slowRequestThreshold = 0;

        /**
         * Headers to exclude from logging (for security)
         * Default: authorization, cookie, set-cookie
         */
        private Set<String> excludedHeaders = Set.of(
                "authorization",
                "cookie",
                "set-cookie"
        );

    }


}
