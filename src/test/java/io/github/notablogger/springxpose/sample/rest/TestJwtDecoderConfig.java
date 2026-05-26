package io.github.notablogger.springxpose.sample.rest;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Replaces the real JwtDecoder (which would try to fetch a JWKS URI at startup)
 * with a no-op stub for all tests.
 *
 * Tests use MockMvcRequestPostProcessors.jwt() to inject a pre-built
 * JwtAuthenticationToken directly into the security context, so the decoder
 * is never actually called — but Spring Boot requires the bean to exist.
 */
@TestConfiguration
public class TestJwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        // Never called in tests — jwt() post-processor bypasses decoding entirely
        return token -> Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", "test")
                .build();
    }
}

