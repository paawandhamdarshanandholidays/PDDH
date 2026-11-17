package com.pddh.auth_service.repository;

import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pddh.auth_service.entities.PasswordResetOtp;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    List<PasswordResetOtp> findAllByEmailIgnoreCaseAndConsumedFalse(String email);
    void deleteAllByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select otp from PasswordResetOtp otp where lower(otp.email) = lower(:email) "
            + "and otp.consumed = false order by otp.createdAt desc")
    List<PasswordResetOtp> findActiveForUpdate(@Param("email") String email);
}
