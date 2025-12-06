package org.lampis.notification.service;

import org.lampis.notification.model.NotificationLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Service for sending email notifications (simulated)
 */
@Service
@Slf4j
public class EmailNotificationService {

    private final Random random = new Random();

    /**
     * Send email notification
     * Simulates sending with 80% success rate for demo purposes
     *
     * @param recipient Email address
     * @param subject Email subject
     * @param message Email body
     * @return true if sent successfully, false otherwise
     */
    public boolean sendEmail(String recipient, String subject, String message) {
        log.info("Attempting to send email to: {}", recipient);
        log.debug("Subject: {}", subject);
        log.debug("Message: {}", message);

        try {
            // Simulate network delay
            Thread.sleep(random.nextInt(100) + 50);

            // Simulate 80% success rate (for demo/testing)
            boolean success = random.nextDouble() < 0.8;

            if (success) {
                log.info("✓ Email sent successfully to: {}", recipient);
                return true;
            } else {
                log.warn("✗ Failed to send email to: {}", recipient);
                return false;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Email sending interrupted for: {}", recipient, e);
            return false;
        } catch (Exception e) {
            log.error("Error sending email to: {}", recipient, e);
            return false;
        }
    }

    /**
     * Send email using notification log
     */
    public boolean sendEmail(NotificationLog log) {
        return sendEmail(log.getRecipient(), log.getSubject(), log.getMessage());
    }
}
