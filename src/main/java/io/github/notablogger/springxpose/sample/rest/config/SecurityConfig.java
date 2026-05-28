package io.github.notablogger.springxpose.sample.rest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;

/**
 * Development-only security configuration for the sample application.
 *
 * This fallback chain permits Swagger UI, API docs, and all API endpoints for demo purposes.
 *
 * ⚠️  IN PRODUCTION: Implement proper authentication and authorization based on your security requirements.
 *     - Enable OAuth2 resource server for JWT validation (uncomment in application.yml)
 *     - Use database-backed user details instead of in-memory users
 *     - Implement selective CSRF protection (not globally disabled)
 *     - Remove in-memory test credentials
 */
@Configuration
@EnableWebSecurity
@Profile("!prod")  // Disable this config in production profile
public class SecurityConfig {

    @Bean
    @Order(1000)
    public SecurityFilterChain defaultChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/swagger-ui/**", "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/actuator/**"
                ).permitAll()
                // Deny by default so only explicitly configured resource chains are reachable.
                .anyRequest().denyAll()
            )
            .csrf(csrf -> csrf.disable())  // ⚠️  DEVELOPMENT ONLY: enable CSRF in production
            .headers(h -> h.frameOptions(fo -> fo.disable()));  // ⚠️  only needed for H2 console in dev
        return http.build();
    }

    /**
     * ⚠️  DEVELOPMENT ONLY: In-memory test users.
     * Replace with database-backed UserDetailsService or OAuth2 in production.
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
            User.withUsername("user")
                .password(encoder.encode("user123"))
                .roles("USER")
                .build(),
            User.withUsername("customer")
                .password(encoder.encode("customer123"))
                .roles("CUSTOMER")
                .build(),
            User.withUsername("admin")
                .password(encoder.encode("admin123"))
                .roles("ADMIN", "USER", "CUSTOMER")
                .build()
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}



