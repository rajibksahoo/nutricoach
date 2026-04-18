package com.nutricoach.library.repository;

import com.nutricoach.library.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    List<Exercise> findByCoachIdAndDeletedAtIsNullOrderByNameAsc(UUID coachId);

    Optional<Exercise> findByIdAndCoachIdAndDeletedAtIsNull(UUID id, UUID coachId);
}
