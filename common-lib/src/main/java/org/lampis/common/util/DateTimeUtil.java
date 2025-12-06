package org.lampis.common.util;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for date and time operations
 */
public class DateTimeUtil {

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private DateTimeUtil() {
        // Utility class
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DEFAULT_FORMATTER) : null;
    }

    public static String formatIso(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(ISO_FORMATTER) : null;
    }

    public static LocalDateTime parse(String dateTimeString) {
        return dateTimeString != null ? LocalDateTime.parse(dateTimeString, DEFAULT_FORMATTER) : null;
    }

    public static LocalDateTime parseIso(String dateTimeString) {
        return dateTimeString != null ? LocalDateTime.parse(dateTimeString, ISO_FORMATTER) : null;
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
