package org.lampis.analytics.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB configuration
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.ecommerce.analytics.repository")
@EnableMongoAuditing
public class MongoDBConfiguration {
    // Spring Boot auto-configuration handles MongoDB connection
    // Additional custom configuration can be added here if needed
}