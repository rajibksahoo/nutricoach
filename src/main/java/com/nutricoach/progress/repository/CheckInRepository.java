package com.nutricoach.progress.repository;

import com.nutricoach.progress.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

    List<CheckIn> findByClientIdAndCoachIdOrderByCheckInDateDesc(UUID clientId, UUID coachId);

    Optional<CheckIn> findByClientIdAndCoachIdAndCheckInDate(UUID clientId, UUID coachId, LocalDate checkInDate);

    boolean existsByClientIdAndCoachIdAndCheckInDate(UUID clientId, UUID coachId, LocalDate checkInDate);
}
