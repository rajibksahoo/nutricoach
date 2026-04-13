package com.nutricoach.library.repository;

import com.nutricoach.library.entity.WorkoutSectionExercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutSectionExerciseRepository extends JpaRepository<WorkoutSectionExercise, UUID> {

    List<WorkoutSectionExercise> findBySectionIdOrderByPositionAsc(UUID sectionId);

    Optional<WorkoutSectionExercise> findByIdAndSectionId(UUID id, UUID sectionId);

    void deleteBySectionId(UUID sectionId);
}
