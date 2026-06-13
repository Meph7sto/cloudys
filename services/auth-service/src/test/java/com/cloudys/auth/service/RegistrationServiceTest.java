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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private AuthUserRepository userRepository;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(userRepository);
    }

    private AuthUser createPendingUser(String id) {
        AuthUser user = new AuthUser();
        user.setUserId(id);
        user.setUsername("user-" + id);
        user.setPasswordHash("hash");
        user.setDisplayName("Pending " + id);
        user.setRole("viewer");
        user.setIsActive(false);
        user.setRegistrationStatus("pending");
        return user;
    }

    @Nested
    @DisplayName("listRegistrations")
    class ListRegistrations {

        @Test
        @DisplayName("should list pending registrations")
        void listPendingRegistrations() {
            when(userRepository.findByRegistrationStatus("pending"))
                    .thenReturn(List.of(createPendingUser("u1"), createPendingUser("u2")));

            var result = registrationService.listRegistrations("pending", 50, 0);
            assertEquals(2, result.size());
            assertEquals("pending", result.get(0).get("registration_status"));
        }

        @Test
        @DisplayName("should show empty list for no registrations")
        void listEmptyRegistrations() {
            when(userRepository.findByRegistrationStatus("pending")).thenReturn(List.of());
            when(userRepository.findByRegistrationStatus("rejected")).thenReturn(List.of());

            var result = registrationService.listRegistrations(null, 50, 0);
            assertEquals(0, result.size());
        }
    }

    @Nested
    @DisplayName("approveRegistration")
    class ApproveRegistration {

        @Test
        @DisplayName("should approve a pending registration")
        void approveSuccess() {
            AuthUser user = createPendingUser("u1");
            when(userRepository.findByIdIncludeInactive("u1")).thenReturn(Optional.of(user));
            when(userRepository.save(any(AuthUser.class))).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = registrationService.approveRegistration(
                    "u1", Constants.ROLE_MEMBER, "CLIENT", "admin1");

            assertEquals("approved", result.get("registration_status"));
            assertEquals(Constants.ROLE_MEMBER, result.get("role"));
            assertEquals("CLIENT", result.get("external_type"));
            assertTrue(user.getIsActive());
        }

        @Test
        @DisplayName("should throw for already approved registration")
        void approveAlreadyProcessed() {
            AuthUser user = createPendingUser("u1");
            user.setRegistrationStatus("approved");
            when(userRepository.findByIdIncludeInactive("u1")).thenReturn(Optional.of(user));

            ErrorResponse ex = assertThrows(ErrorResponse.class, () ->
                    registrationService.approveRegistration("u1", Constants.ROLE_MEMBER, null, "admin1"));
            assertTrue(ex.getDetail().contains("已处理"));
        }
    }

    @Nested
    @DisplayName("rejectRegistration")
    class RejectRegistration {

        @Test
        @DisplayName("should reject a pending registration with reason")
        void rejectWithReason() {
            AuthUser user = createPendingUser("u1");
            when(userRepository.findByIdIncludeInactive("u1")).thenReturn(Optional.of(user));
            when(userRepository.save(any(AuthUser.class))).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = registrationService.rejectRegistration(
                    "u1", "Not qualified", "admin1");

            assertEquals("rejected", result.get("registration_status"));
            assertEquals("Not qualified", result.get("rejection_reason"));
        }

        @Test
        @DisplayName("should throw for blank rejection reason")
        void rejectBlankReason() {
            AuthUser user = createPendingUser("u1");
            when(userRepository.findByIdIncludeInactive("u1")).thenReturn(Optional.of(user));

            assertThrows(ErrorResponse.class, () ->
                    registrationService.rejectRegistration("u1", "  ", "admin1"));
        }
    }

    @Nested
    @DisplayName("getPendingCount")
    class GetPendingCount {

        @Test
        @DisplayName("should return correct pending count")
        void pendingCount() {
            when(userRepository.countByRegistrationStatus("pending")).thenReturn(5L);
            assertEquals(5L, registrationService.getPendingCount());
        }
    }
}
