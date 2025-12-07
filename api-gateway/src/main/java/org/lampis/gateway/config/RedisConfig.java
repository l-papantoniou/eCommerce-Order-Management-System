package org.lampis.gateway.config;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis configuration for rate limiting
 */
@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Creates Redis client for Bucket4j
     * This client stores token buckets in Redis
     */
    @Bean
    public RedisClient redisClient() {
        // Build Redis URI
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(redisHost)
                .withPort(redisPort);

        // Add password if configured
        if (redisPassword != null && !redisPassword.isEmpty()) {
            builder.withPassword(redisPassword.toCharArray());
        }

        RedisURI redisURI = builder.build();
        RedisClient client = RedisClient.create(redisURI);

        log.info("Redis client configured: {}:{}", redisHost, redisPort);

        return client;
    }
}