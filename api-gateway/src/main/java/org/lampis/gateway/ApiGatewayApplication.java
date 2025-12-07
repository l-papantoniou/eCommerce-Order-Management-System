package org.lampis.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway Application
 * <p>
 * Entry point for all client requests to backend microservices.
 * Provides:
 * - JWT authentication
 * - Rate limiting
 * - Request routing
 * - Request/response logging
 * - Correlation ID tracking
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}