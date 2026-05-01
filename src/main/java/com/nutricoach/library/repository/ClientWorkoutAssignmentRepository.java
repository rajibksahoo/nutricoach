package com.nutricoach.library.repository;

import com.nutricoach.library.entity.ClientWorkoutAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientWorkoutAssignmentRepository extends JpaRepository<ClientWorkoutAssignment, UUID> {

    List<ClientWorkoutAssignment> findByWorkoutIdAndDeletedAtIsNull(UUID workoutId);

    List<ClientWorkoutAssignment> findByCoachIdAndClientIdAndDeletedAtIsNullOrderByAssignedAtDesc(UUID coachId, UUID clientId);

    Optional<ClientWorkoutAssignment> findByIdAndCoachIdAndDeletedAtIsNull(UUID id, UUID coachId);

    Optional<ClientWorkoutAssignment> findByClientIdAndWorkoutIdAndDeletedAtIsNull(UUID clientId, UUID workoutId);
}
