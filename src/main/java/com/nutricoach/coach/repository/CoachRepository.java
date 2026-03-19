package com.nutricoach.coach.repository;

import com.nutricoach.coach.entity.Coach;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CoachRepository extends JpaRepository<Coach, UUID> {
    Optional<Coach> findByPhone(String phone);
    boolean existsByPhone(String phone);
}
