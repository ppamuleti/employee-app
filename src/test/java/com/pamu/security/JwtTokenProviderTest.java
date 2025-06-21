package com.pamu.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String SECRET_KEY = "testsecretkeytestsecretkeytestsecretkeytestsecretkey";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        // If JwtTokenProvider allows setting SECRET_KEY, set it here for test predictability
        // Otherwise, ensure the test uses the same key as the provider
    }

    @Test
    void testGenerateAndValidateToken() {
        // Use the provider to generate and validate the token for consistency
        com.pamu.model.User user = new com.pamu.model.User();
        user.setUsername("testuser");
        String token = jwtTokenProvider.generateToken(user);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("testuser", jwtTokenProvider.extractUsername(token));
    }

    @Test
    void testValidateToken_invalidToken() {
        String invalidToken = "invalid.token.value";
        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }

    @Test
    void testValidateToken_expiredToken() throws InterruptedException {
        String username = "expireduser";
        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis() - 2000))
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS256)
                .compact();
        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    void testExtractUsername_invalidToken() {
        String invalidToken = "invalid.token.value";
        assertThrows(Exception.class, () -> jwtTokenProvider.extractUsername(invalidToken));
    }
}
