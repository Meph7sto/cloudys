package com.cloudys.auth.repository;

import com.cloudys.auth.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, String> {

    Optional<AuthUser> findByUsername(String username);

    @Query("SELECT u FROM AuthUser u WHERE u.username = :username")
    Optional<AuthUser> findByUsernameForAuth(@Param("username") String username);

    @Query("SELECT u FROM AuthUser u WHERE "
            + "(:includeInactive = true OR u.isActive = true) "
            + "AND (:role IS NULL OR u.role = :role) "
            + "ORDER BY u.createdAt DESC")
    List<AuthUser> listUsers(@Param("role") String role,
                             @Param("includeInactive") boolean includeInactive);

    @Query("SELECT COUNT(u) FROM AuthUser u WHERE "
            + "(:role IS NULL OR u.role = :role)")
    long countUsers(@Param("role") String role);

    @Query("SELECT u FROM AuthUser u WHERE u.registrationStatus = :status "
            + "ORDER BY u.createdAt DESC")
    List<AuthUser> findByRegistrationStatus(@Param("status") String status);

    @Query("SELECT COUNT(u) FROM AuthUser u WHERE u.registrationStatus = :status")
    long countByRegistrationStatus(@Param("status") String status);

    @Modifying
    @Query("UPDATE AuthUser u SET u.isActive = false, u.updatedAt = :now WHERE u.userId = :userId")
    void softDelete(@Param("userId") String userId, @Param("now") java.time.Instant now);

    @Modifying
    @Query("UPDATE AuthUser u SET u.passwordHash = :newHash, u.updatedAt = :now WHERE u.userId = :userId")
    void changePassword(@Param("userId") String userId, @Param("newHash") String newHash, @Param("now") java.time.Instant now);

    @Query("SELECT u FROM AuthUser u WHERE u.userId = :userId")
    Optional<AuthUser> findByIdIncludeInactive(@Param("userId") String userId);
}
