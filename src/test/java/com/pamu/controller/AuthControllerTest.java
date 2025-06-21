package com.pamu.controller;

import com.pamu.model.User;
import com.pamu.model.UserLoginRequest;
import com.pamu.repository.UserRepository;
import com.pamu.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testLogin_Success() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("hashedPassword");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken(user)).thenReturn("mockToken");

        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");
        ResponseEntity<?> response = authController.login(loginRequest);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        assertTrue(response.getHeaders().getFirst(HttpHeaders.AUTHORIZATION).contains("Bearer mockToken"));
        assertTrue(response.getBody() instanceof java.util.Map);
        assertEquals("mockToken", ((java.util.Map<?, ?>)response.getBody()).get("token"));
    }

    @Test
    void testLogin_Failure_WrongPassword() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("hashedPassword");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("wrongpassword");
        ResponseEntity<?> response = authController.login(loginRequest);
        assertEquals(401, response.getStatusCodeValue());
        assertEquals("Invalid username or password.", response.getBody());
    }

    @Test
    void testLogin_Failure_UserNotFound() {
        when(userRepository.findByUsername("nouser")).thenReturn(Optional.empty());
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUsername("nouser");
        loginRequest.setPassword("password");
        ResponseEntity<?> response = authController.login(loginRequest);
        assertEquals(401, response.getStatusCodeValue());
        assertEquals("Invalid username or password.", response.getBody());
    }

    @Test
    void testLogout_Success() {
        doNothing().when(jwtTokenProvider).invalidateToken("Bearer mockToken");
        ResponseEntity<String> response = authController.logout("Bearer mockToken");
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("User logged out successfully.", response.getBody());
        verify(jwtTokenProvider, times(1)).invalidateToken("Bearer mockToken");
    }
}
