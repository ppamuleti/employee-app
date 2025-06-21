package com.pamu.security;

import com.pamu.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Component
public class JwtTokenProvider {

    @Autowired UserDetailsService userDetailsService;

    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS512); // Generates secure 512-bit key
    private static final long EXPIRATION_TIME = 86400000L; // 1 day

    private final Set<String> invalidTokens = new HashSet<>();

    /**
     * Generates a JWT token for the given user.
     * This method is developed to provide secure, stateless authentication for the application using JWT.
     * @param user The user for whom the token is generated
     * @return The generated JWT token as a String
     */
    // Generate JWT Token
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
                .compact();
    }

    /**
     * Extracts the username from the given JWT token.
     * This method is developed to retrieve the user identity from the token for authorization and validation.
     * @param token The JWT token
     * @return The username extracted from the token
     */
    // Extract Username
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Validates the given JWT token for authenticity and expiration.
     * This method is developed to ensure that only valid and non-expired tokens are accepted for authentication.
     * @param token The JWT token to validate
     * @return true if the token is valid and not expired, false otherwise
     */
    // Validate Token
    public boolean validateToken(String token) {
        if (invalidTokens.contains(token)) return false;
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Extract Claims
    private Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY).build()
                .parseClaimsJws(token)
                .getBody();
    }

    // **Fix: Implementing getAuthentication method**
    public Authentication getAuthentication(String token) {
        String username = extractUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    public void invalidateToken(String token) {
        invalidTokens.add(token);
    }
}
