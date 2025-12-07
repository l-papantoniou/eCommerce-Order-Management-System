package org.lampis.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Analytics Service Application
 */
@SpringBootApplication(scanBasePackages = {
        "com.ecommerce.analytics",
        "com.ecommerce.common"
})
public class AnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}