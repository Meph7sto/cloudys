package com.cloudys.auth.service;

import com.cloudys.auth.entity.AuthUser;
import com.cloudys.auth.repository.AuthUserRepository;
import com.cloudys.common.core.constant.Constants;
import com.cloudys.common.core.exception.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AuthUserRepository userRepository;

    @Mock
    private AuthService authService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, authService);
    }

    private AuthUser createUser(String id, String role, boolean active) {
        AuthUser user = new AuthUser();
        user.setUserId(id);
        user.setUsername("user-" + id);
        user.setPasswordHash("hash");
        user.setDisplayName("Display " + id);
        user.setRole(role);
        user.setIsActive(active);
        user.setRegistrationStatus(active ? "approved" : "pending");
        return user;
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("should return active user")
        void getActiveUser() {
            AuthUser user = createUser("u1", "member", true);
            when(userRepository.findById("u1")).thenReturn(Optional.of(user));

            AuthUser result = userService.getById("u1");
            assertEquals("u1", result.getUserId());
        }

        @Test
        @DisplayName("should throw 404 for non-existent user")
        void getNonExistentUser() {
            when(userRepository.findById("u1")).thenReturn(Optional.empty());
            ErrorResponse ex = assertThrows(ErrorResponse.class, () -> userService.getById("u1"));
            assertEquals(404, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("super_admin can create admin user")
        void superAdminCanCreateAdmin() {
            AuthUser creator = createUser("super", "super_admin", true);
            when(userRepository.findById("super")).thenReturn(Optional.of(creator));
            when(userRepository.findByUsername("newadmin")).thenReturn(Optional.empty());
            when(authService.generateUserId()).thenReturn("gen-id");
            when(authService.hashPassword(any())).thenReturn("hashed");
            when(userRepository.save(any(AuthUser.class))).thenAnswer(inv -> inv.getArgument(0));

            AuthUser result = userService.createUser("newadmin", "password", "New Admin",
                    Constants.ROLE_ADMIN, null, "super");
            assertEquals(Constants.ROLE_ADMIN, result.getRole());
        }

        @Test
        @DisplayName("admin cannot create super_admin")
        void adminCannotCreateSuperAdmin() {
            AuthUser creator = createUser("admin1", "admin", true);
            when(userRepository.findById("admin1")).thenReturn(Optional.of(creator));

            assertThrows(ErrorResponse.class, () ->
                    userService.createUser("newsa", "password", "SA",
                            Constants.ROLE_SUPER_ADMIN, null, "admin1"));
        }

        @Test
        @DisplayName("should throw for existing username")
        void createExistingUsername() {
            when(userRepository.findByUsername("existing")).thenReturn(Optional.of(mock(AuthUser.class)));

            assertThrows(ErrorResponse.class, () ->
                    userService.createUser("existing", "password", "Dup",
                            Constants.ROLE_MEMBER, null, "super"));
        }
    }

    @Nested
    @DisplayName("listUsers")
    class ListUsers {

        @Test
        @DisplayName("should return filtered user list")
        void listUsersFiltered() {
            when(userRepository.listUsers("member", false))
                    .thenReturn(List.of(createUser("u1", "member", true)));

            var result = userService.listUsers("member", false);
            assertEquals(1, result.size());
            assertEquals("member", result.get(0).get("role"));
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("should update password when old password matches")
        void changePasswordSuccess() {
            AuthUser user = createUser("u1", "member", true);
            when(userRepository.findById("u1")).thenReturn(Optional.of(user));
            when(authService.verifyPassword("old", "hash")).thenReturn(true);
            when(authService.hashPassword("newPass")).thenReturn("newHash");

            userService.changePassword("u1", "old", "newPass");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("should throw for wrong old password")
        void changePasswordWrongOld() {
            AuthUser user = createUser("u1", "member", true);
            when(userRepository.findById("u1")).thenReturn(Optional.of(user));
            when(authService.verifyPassword("wrong", "hash")).thenReturn(false);

            ErrorResponse ex = assertThrows(ErrorResponse.class,
                    () -> userService.changePassword("u1", "wrong", "newPass"));
            assertEquals(400, ex.getStatusCode());
        }

        @Test
        @DisplayName("should throw for short new password")
        void changePasswordShortNew() {
            AuthUser user = createUser("u1", "member", true);
            when(userRepository.findById("u1")).thenReturn(Optional.of(user));
            when(authService.verifyPassword("old", "hash")).thenReturn(true);

            assertThrows(ErrorResponse.class,
                    () -> userService.changePassword("u1", "old", "12345"));
        }
    }
}
