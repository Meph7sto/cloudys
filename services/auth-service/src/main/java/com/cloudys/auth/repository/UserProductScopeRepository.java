package com.cloudys.auth.repository;

import com.cloudys.auth.entity.UserProductScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProductScopeRepository extends JpaRepository<UserProductScope, Long> {

    List<UserProductScope> findByUserId(String userId);

    @Modifying
    @Query("DELETE FROM UserProductScope s WHERE s.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);

    boolean existsByUserIdAndProductId(String userId, String productId);

    @Query("SELECT s.productId FROM UserProductScope s WHERE s.userId = :userId")
    List<String> findProductIdsByUserId(@Param("userId") String userId);

    @Query("SELECT s.canEdit FROM UserProductScope s WHERE s.userId = :userId AND s.productId = :productId")
    Optional<Boolean> findCanEditByUserIdAndProductId(@Param("userId") String userId,
                                                       @Param("productId") String productId);
}
