package com.nutricoach.library.repository;

import com.nutricoach.library.entity.WorkoutSectionAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutSectionAssignmentRepository extends JpaRepository<WorkoutSectionAssignment, UUID> {

    List<WorkoutSectionAssignment> findByWorkoutIdOrderByPositionAsc(UUID workoutId);

    Optional<WorkoutSectionAssignment> findByIdAndWorkoutId(UUID id, UUID workoutId);

    boolean existsBySectionId(UUID sectionId);

    void deleteByWorkoutId(UUID workoutId);
}
