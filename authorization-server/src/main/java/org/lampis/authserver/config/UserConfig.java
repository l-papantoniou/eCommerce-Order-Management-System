package org.lampis.authserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * User Configuration
 * <p>
 * Configures in-memory users for authentication.
 * <p>
 * Demo Users:
 * 1. admin@ecommerce.com - Admin user with full access
 * 2. john@ecommerce.com - Regular user
 * 3. jane@ecommerce.com - Regular user
 * <p>
 * Current Implementation: In-Memory Storage
 * Production Alternative: Database Storage (JdbcUserDetailsManager or custom UserDetailsService)
 */
@Configuration
public class UserConfig {

    /**
     * Password Encoder Bean
     * Uses BCrypt hashing algorithm for password storage
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * User Details Service
     * <p>
     * Provides user authentication details for the authorization server.
     * In production, this should be replaced with a database-backed implementation.
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {

        // Admin User - Full permissions
        UserDetails admin = User.builder()
                .username("admin@ecommerce.com")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN", "USER")
                .authorities("ROLE_ADMIN", "ROLE_USER", "admin.read", "admin.write", "orders.read", "orders.write")
                .build();

        // Regular User 1 - John
        UserDetails john = User.builder()
                .username("john@ecommerce.com")
                .password(passwordEncoder.encode("john123"))
                .roles("USER")
                .authorities("ROLE_USER", "orders.read", "orders.write")
                .build();

        // Regular User 2 - Jane
        UserDetails jane = User.builder()
                .username("jane@ecommerce.com")
                .password(passwordEncoder.encode("jane123"))
                .roles("USER")
                .authorities("ROLE_USER", "orders.read")
                .build();

        return new InMemoryUserDetailsManager(admin, john, jane);
    }
}