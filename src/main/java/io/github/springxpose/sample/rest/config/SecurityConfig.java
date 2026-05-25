package io.github.springxpose.sample.rest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * Fallback / default security chain.
 *
 * <p>Generated per-entity chains (from @ExposeEntity) use @Order(1xx) and match
 * only their own paths, so they take priority.  This chain covers everything else
 * (Swagger UI, H2 console, actuator, etc.) and permits it all for local dev.
 *
 * <p>It also declares the in-memory users needed by the Basic-auth secured
 * endpoints (Order and its chain).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Low-priority fallback chain — permits Swagger UI, H2 console, etc. */
    @Bean
    @Order(1000)
    public SecurityFilterChain defaultChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/swagger-ui/**", "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/h2-console/**",
                    "/actuator/**"
                ).permitAll()
                .anyRequest().permitAll()
            )
            .csrf(csrf -> csrf.disable())
            .headers(h -> h.frameOptions(fo -> fo.disable())); // H2 console uses iframes
        return http.build();
    }

    /** In-memory users for Basic-auth scenarios. */
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
