package com.cloudys.auth.service;

import com.cloudys.auth.entity.AuthUser;
import com.cloudys.auth.repository.AuthUserRepository;
import com.cloudys.common.core.exception.ErrorResponse;
import com.cloudys.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthUserRepository userRepository;

    private AuthService authService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(
                "test-secret-key-for-unit-test-1234567890", 24);
        authService = new AuthService(userRepository, tokenProvider);
    }

    @Test
    @DisplayName("hashPassword should produce valid BCrypt hash")
    void hashPasswordShouldProduceBCryptHash() {
        String hash = authService.hashPassword("test123456");
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2"));
        assertTrue(encoder.matches("test123456", hash));
    }

    @Test
    @DisplayName("verifyPassword should return true for matching password")
    void verifyPasswordShouldMatchCorrectPassword() {
        String hash = encoder.encode("correctPassword");
        assertTrue(authService.verifyPassword("correctPassword", hash));
    }

    @Test
    @DisplayName("verifyPassword should return false for wrong password")
    void verifyPasswordShouldRejectWrongPassword() {
        String hash = encoder.encode("correctPassword");
        assertFalse(authService.verifyPassword("wrongPassword", hash));
    }

    @Test
    @DisplayName("generateUserId should produce unique IDs with user- prefix")
    void generateUserIdShouldProduceUniqueIds() {
        String id1 = authService.generateUserId();
        String id2 = authService.generateUserId();
        assertNotNull(id1);
        assertTrue(id1.startsWith("user-"));
        assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("login should return token for valid credentials")
    void loginShouldReturnTokenForValidCredentials() {
        AuthUser user = createApprovedUser("testuser", "super_admin");
        when(userRepository.findByUsernameForAuth("testuser")).thenReturn(Optional.of(user));

        var result = authService.login("testuser", "password", null);

        assertNotNull(result);
        assertEquals("testuser", result.get("username"));
        assertNotNull(result.get("token"));
        assertEquals("super_admin", result.get("role"));
        verify(userRepository).findByUsernameForAuth("testuser");
    }

    @Test
    @DisplayName("login should throw for wrong password")
    void loginShouldThrowForWrongPassword() {
        AuthUser user = createApprovedUser("testuser", "member");
        // User has password "password" but we try "wrong"
        when(userRepository.findByUsernameForAuth("testuser")).thenReturn(Optional.of(user));

        assertThrows(ErrorResponse.class, () -> authService.login("testuser", "wrong", null));
    }

    @Test
    @DisplayName("login should throw for pending registration")
    void loginShouldThrowForPendingRegistration() {
        AuthUser user = createApprovedUser("testuser", "member");
        user.setRegistrationStatus("pending");
        user.setIsActive(false);
        when(userRepository.findByUsernameForAuth("testuser")).thenReturn(Optional.of(user));

        ErrorResponse ex = assertThrows(ErrorResponse.class,
                () -> authService.login("testuser", "password", null));
        assertTrue(ex.getDetail().contains("审核中"));
    }

    @Test
    @DisplayName("register should create user with pending status")
    void registerShouldCreatePendingUser() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.save(any(AuthUser.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = authService.register("newuser", "password123", "New User");

        assertNotNull(result);
        assertEquals("newuser", result.get("username"));
        assertEquals("pending", result.get("registration_status"));
        verify(userRepository).save(any(AuthUser.class));
    }

    @Test
    @DisplayName("register should throw for existing username")
    void registerShouldThrowForExistingUsername() {
        AuthUser existing = new AuthUser();
        existing.setUsername("existing");
        when(userRepository.findByUsername("existing")).thenReturn(Optional.of(existing));

        assertThrows(ErrorResponse.class,
                () -> authService.register("existing", "password123", "Display"));
    }

    @Test
    @DisplayName("register should throw for short password")
    void registerShouldThrowForShortPassword() {
        assertThrows(ErrorResponse.class,
                () -> authService.register("user", "12345", "Display"));
    }

    // Helper
    private AuthUser createApprovedUser(String username, String role) {
        AuthUser user = new AuthUser();
        user.setUserId("user-" + username);
        user.setUsername(username);
        user.setPasswordHash(encoder.encode("password"));
        user.setDisplayName(username);
        user.setRole(role);
        user.setIsActive(true);
        user.setRegistrationStatus("approved");
        return user;
    }
}
