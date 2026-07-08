package com.nutricoach.library.repository;

import com.nutricoach.library.entity.ClientWorkoutCompletion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * All queries are tenant-scoped: they always filter by both coachId and clientId
 * (multi-tenant rule) plus the soft-delete guard.
 */
public interface ClientWorkoutCompletionRepository extends JpaRepository<ClientWorkoutCompletion, UUID> {

    List<ClientWorkoutCompletion> findByCoachIdAndClientIdAndDeletedAtIsNull(UUID coachId, UUID clientId);

    Optional<ClientWorkoutCompletion> findByCoachIdAndClientIdAndWorkoutIdAndWorkoutDateAndDeletedAtIsNull(
            UUID coachId, UUID clientId, UUID workoutId, LocalDate workoutDate);
}
