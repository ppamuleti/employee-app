package com.pamu.controller;

import com.pamu.model.UserLoginRequest;
import com.pamu.security.JwtTokenProvider;
import com.pamu.model.User;
import com.pamu.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired UserRepository userRepository;
    @Autowired BCryptPasswordEncoder passwordEncoder;

    /**
     * Authenticates the user and generates a JWT token if credentials are valid.
     * This method is developed to provide secure login and token-based authentication for the application.
     * @param loginRequest The login request containing username and password
     * @return ResponseEntity with JWT token in the header if successful, or 401 status if authentication fails
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequest loginRequest) {
        Optional<User> user = userRepository.findByUsername(loginRequest.getUsername());
        if (user.isPresent() && passwordEncoder.matches(loginRequest.getPassword(), user.get().getPassword())) {
            String token = jwtTokenProvider.generateToken(user.get());
            return ResponseEntity.ok().header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .body(java.util.Collections.singletonMap("token", token));
        }
        return ResponseEntity.status(401).body("Invalid username or password.");
    }

    /**
     * Invalidates the JWT token to log out the user.
     * This method is developed to support secure logout and token invalidation for session management.
     * @param token The JWT token to invalidate (from Authorization header)
     * @return ResponseEntity with logout success message
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        jwtTokenProvider.invalidateToken(token);
        return ResponseEntity.ok("User logged out successfully.");
    }
}
