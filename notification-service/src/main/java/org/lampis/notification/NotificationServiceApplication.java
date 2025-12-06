package org.lampis.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.Retryable;

/**
 * Notification Service Application
 */
@SpringBootApplication(scanBasePackages = {
        "com.ecommerce.notification",
        "com.ecommerce.common"
})
@Retryable
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}

