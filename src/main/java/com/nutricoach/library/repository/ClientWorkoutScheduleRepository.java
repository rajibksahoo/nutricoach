package com.nutricoach.library.repository;

import com.nutricoach.library.entity.ClientWorkoutSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientWorkoutScheduleRepository extends JpaRepository<ClientWorkoutSchedule, UUID> {

    List<ClientWorkoutSchedule> findByCoachIdAndClientIdAndScheduledDateBetweenAndDeletedAtIsNullOrderByScheduledDateAsc(
            UUID coachId, UUID clientId, LocalDate from, LocalDate to);

    List<ClientWorkoutSchedule> findByCoachIdAndClientIdAndDeletedAtIsNullOrderByScheduledDateAsc(UUID coachId, UUID clientId);

    Optional<ClientWorkoutSchedule> findByIdAndCoachIdAndDeletedAtIsNull(UUID id, UUID coachId);
}
