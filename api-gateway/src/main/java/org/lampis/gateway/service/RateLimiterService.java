package org.lampis.gateway.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter service using Token Bucket algorithm with Redis
 */
@Slf4j
@Service
public class RateLimiterService {

    // Redis proxy manager - manages token buckets in Redis
    private final LettuceBasedProxyManager<String> proxyManager;

    // Cache bucket configurations to avoid recreating them
    private final ConcurrentHashMap<String, BucketConfiguration> configCache = new ConcurrentHashMap<>();

    public RateLimiterService(RedisClient redisClient) {
        // Create Redis connection with proper codecs
        StatefulRedisConnection<String, byte[]> connection =
                redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        // Create a proxy manager - this handles storing buckets in Redis
        this.proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(
                        // Expire unused buckets after 1 hour of inactivity
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                                Duration.ofHours(1)
                        )
                )
                .build();

        log.info("Rate Limiter service initialized with Redis");
    }

    /**
     * Check if request is allowed based on rate limit
     */
    public boolean isAllowed(String key, int capacity, int refillTokens, Duration refillDuration) {
        BucketConfiguration config = getBucketConfiguration(capacity, refillTokens, refillDuration);
        Bucket bucket = proxyManager.builder().build(key, config);

        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn("Rate limit exceeded for key: {}", key);
        } else {
            log.debug("Rate limit check passed for key: {}", key);
        }

        return allowed;
    }

    /**
     * Get remaining tokens for a key
     */
    public long getRemainingTokens(String key, int capacity, int refillTokens, Duration refillDuration) {
        BucketConfiguration config = getBucketConfiguration(capacity, refillTokens, refillDuration);
        Bucket bucket = proxyManager.builder().build(key, config);
        return bucket.getAvailableTokens();
    }

    /**
     * Get or create bucket configuration
     */
    private BucketConfiguration getBucketConfiguration(int capacity, int refillTokens, Duration refillDuration) {
        String configKey = generateConfigKey(capacity, refillTokens, refillDuration);
        return configCache.computeIfAbsent(configKey, k ->
                createBucketConfiguration(capacity, refillTokens, refillDuration)
        );
    }

    /**
     * Create bucket configuration with Token Bucket parameters
     */
    private BucketConfiguration createBucketConfiguration(int capacity, int refillTokens, Duration refillDuration) {
        Bandwidth bandwidth = Bandwidth.classic(
                capacity,
                Refill.intervally(refillTokens, refillDuration)
        );

        return BucketConfiguration.builder()
                .addLimit(bandwidth)
                .build();
    }

    /**
     * Generate unique cache key for bucket configuration
     */
    private String generateConfigKey(int capacity, int refillTokens, Duration refillDuration) {
        return String.format("config:%d:%d:%d", capacity, refillTokens, refillDuration.getSeconds());
    }
}