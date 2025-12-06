package org.lampis.notification.service;

import org.lampis.notification.model.NotificationLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Service for sending SMS notifications (simulated)
 */
@Service
@Slf4j
public class SmsNotificationService {

    private final Random random = new Random();

    /**
     * Send SMS notification
     * Simulates sending with 85% success rate for demo purposes
     *
     * @param phoneNumber Phone number
     * @param message SMS message
     * @return true if sent successfully, false otherwise
     */
    public boolean sendSms(String phoneNumber, String message) {
        log.info("Attempting to send SMS to: {}", phoneNumber);
        log.debug("Message: {}", message);

        try {
            // Simulate network delay
            Thread.sleep(random.nextInt(100) + 30);

            // Simulate 85% success rate (for demo/testing)
            boolean success = random.nextDouble() < 0.85;

            if (success) {
                log.info("✓ SMS sent successfully to: {}", phoneNumber);
                return true;
            } else {
                log.warn("✗ Failed to send SMS to: {}", phoneNumber);
                return false;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("SMS sending interrupted for: {}", phoneNumber, e);
            return false;
        } catch (Exception e) {
            log.error("Error sending SMS to: {}", phoneNumber, e);
            return false;
        }
    }

    /**
     * Send SMS using notification log
     */
    public boolean sendSms(NotificationLog log) {
        return sendSms(log.getRecipient(), log.getMessage());
    }
}
