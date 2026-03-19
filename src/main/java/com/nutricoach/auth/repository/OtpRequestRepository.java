package com.nutricoach.auth.repository;

import com.nutricoach.auth.entity.OtpRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface OtpRequestRepository extends JpaRepository<OtpRequest, UUID> {

    Optional<OtpRequest> findTopByPhoneOrderByCreatedAtDesc(String phone);

    @Modifying
    @Query("DELETE FROM OtpRequest o WHERE o.phone = :phone AND o.verified = false")
    void deleteUnverifiedByPhone(String phone);
}
