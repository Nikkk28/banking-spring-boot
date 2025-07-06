package com.banking.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;
    private final String testSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final int jwtExpiration = 3600; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", testSecret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", jwtExpiration);
    }

    @Test
    void generateToken_Success() {
        // Given
        String username = "testuser";

        // When
        String token = jwtService.generateToken(username);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Verify token can be parsed and contains correct username
        String extractedUsername = jwtService.extractUsername(token);
        assertEquals(username, extractedUsername);
    }

    @Test
    void generateToken_WithExtraClaims() {
        // Given
        String username = "testuser";
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "CUSTOMER");
        extraClaims.put("userId", 123L);

        // When
        String token = jwtService.generateToken(extraClaims, username);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());

        String extractedUsername = jwtService.extractUsername(token);
        assertEquals(username, extractedUsername);

        // Verify extra claims are present
        Claims claims = extractAllClaims(token);
        assertEquals("CUSTOMER", claims.get("role"));
        assertEquals(123, claims.get("userId"));
    }

    @Test
    void extractUsername_Success() {
        // Given
        String username = "testuser";
        String token = jwtService.generateToken(username);

        // When
        String extractedUsername = jwtService.extractUsername(token);

        // Then
        assertEquals(username, extractedUsername);
    }

    @Test
    void extractClaim_Success() {
        // Given
        String username = "testuser";
        String token = jwtService.generateToken(username);

        // When
        String extractedUsername = jwtService.extractClaim(token, Claims::getSubject);
        Date issuedAt = jwtService.extractClaim(token, Claims::getIssuedAt);
        Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

        // Then
        assertEquals(username, extractedUsername);
        assertNotNull(issuedAt);
        assertNotNull(expiration);
        assertTrue(expiration.after(issuedAt));
    }

    @Test
    void isTokenValid_ValidToken() {
        // Given
        String username = "testuser";
        String token = jwtService.generateToken(username);

        // When
        boolean isValid = jwtService.isTokenValid(token, username);

        // Then
        assertTrue(isValid);
    }

    @Test
    void isTokenValid_InvalidUsername() {
        // Given
        String username = "testuser";
        String differentUsername = "differentuser";
        String token = jwtService.generateToken(username);

        // When
        boolean isValid = jwtService.isTokenValid(token, differentUsername);

        // Then
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_ExpiredToken() {
        // Given
        String username = "testuser";
        JwtService shortExpirationService = new JwtService();
        ReflectionTestUtils.setField(shortExpirationService, "secret", testSecret);
        ReflectionTestUtils.setField(shortExpirationService, "jwtExpiration", -1); // Expired immediately

        String expiredToken = shortExpirationService.generateToken(username);

        // When
        boolean isValid = jwtService.isTokenValid(expiredToken, username);

        // Then
        assertFalse(isValid);
    }

    @Test
    void extractUsername_InvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            jwtService.extractUsername(invalidToken);
        });
    }

    @Test
    void generateToken_EmptyUsername() {
        // Given
        String username = "";

        // When
        String token = jwtService.generateToken(username);

        // Then
        assertNotNull(token);
        String extractedUsername = jwtService.extractUsername(token);
        assertEquals(username, extractedUsername);
    }

    @Test
    void generateToken_NullUsername() {
        // Given
        String username = null;

        // When
        String token = jwtService.generateToken(username);

        // Then
        assertNotNull(token);
        String extractedUsername = jwtService.extractUsername(token);
        assertEquals(username, extractedUsername);
    }

    @Test
    void tokenExpiration_CorrectDuration() {
        // Given
        String username = "testuser";
        long beforeGeneration = System.currentTimeMillis();

        // When
        String token = jwtService.generateToken(username);
        long afterGeneration = System.currentTimeMillis();

        // Then
        Date expiration = jwtService.extractClaim(token, Claims::getExpiration);
        long expirationTime = expiration.getTime();

        // The expiration should be approximately jwtExpiration seconds after generation
        long expectedMinExpiration = beforeGeneration + (jwtExpiration * 1000L);
        long expectedMaxExpiration = afterGeneration + (jwtExpiration * 1000L);

        assertTrue(expirationTime >= expectedMinExpiration);
        assertTrue(expirationTime <= expectedMaxExpiration);
    }

    @Test
    void isTokenValid_NullToken() {
        // Given
        String username = "testuser";
        String token = null;

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            jwtService.isTokenValid(token, username);
        });
    }

    @Test
    void isTokenValid_EmptyToken() {
        // Given
        String username = "testuser";
        String token = "";

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            jwtService.isTokenValid(token, username);
        });
    }

    // Helper method to extract all claims (using the same key as the service)
    private Claims extractAllClaims(String token) {
        Key key = Keys.hmacShaKeyFor(testSecret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}