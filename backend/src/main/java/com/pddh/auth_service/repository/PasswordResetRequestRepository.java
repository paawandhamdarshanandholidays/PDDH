package com.pddh.auth_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pddh.auth_service.entities.PasswordResetRequest;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {
    List<PasswordResetRequest> findAllByStatusOrderByRequestedAtAsc(String status);
    boolean existsByTargetUserIdAndStatus(Long targetUserId, String status);

    @Modifying
    @Query("delete from PasswordResetRequest request where request.targetUser.id = :userId "
            + "or request.requester.id = :userId or request.reviewer.id = :userId")
    void deleteAllReferencingUser(@Param("userId") Long userId);
}
