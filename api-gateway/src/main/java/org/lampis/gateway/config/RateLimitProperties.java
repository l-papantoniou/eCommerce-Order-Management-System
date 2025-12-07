package org.lampis.gateway.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for rate limiting
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    /**
     * Enable/disable rate limiting globally
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Default bucket capacity (max tokens)
     * Example: 100 means user can make 100 requests immediately
     */
    @Min(1)
    private int defaultCapacity = 100;

    /**
     * Default refill tokens (how many tokens to add)
     * Example: 100 means add 100 tokens per refill
     */
    @Min(1)
    private int defaultRefillTokens = 100;

    /**
     * Default refill duration in seconds
     * Example: 60 means refill every 60 seconds
     */
    @Min(1)
    private int defaultRefillDuration = 60;
}