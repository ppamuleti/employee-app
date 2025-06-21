package com.pamu.configuration;

import com.pamu.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityConfigTest {
    @Test
    void testPasswordEncoderBean() {
        SecurityConfig config = new SecurityConfig();
        BCryptPasswordEncoder encoder = config.passwordEncoder();
        assertNotNull(encoder);
        assertTrue(encoder.matches("password", encoder.encode("password")));
    }

    @Test
    void testJwtAuthenticationFilterBean() {
        SecurityConfig config = new SecurityConfig();
        JwtAuthenticationFilter filter = config.jwtAuthenticationFilter();
        assertNotNull(filter);
    }

    @Test
    void testAuthenticationManagerBean() throws Exception {
        SecurityConfig config = new SecurityConfig();
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);
        assertEquals(authenticationManager, config.authenticationManager(authenticationConfiguration));
    }
}

