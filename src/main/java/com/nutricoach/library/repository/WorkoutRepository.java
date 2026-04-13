package com.nutricoach.library.repository;

import com.nutricoach.library.entity.Workout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutRepository extends JpaRepository<Workout, UUID> {

    List<Workout> findByCoachIdAndDeletedAtIsNullOrderByNameAsc(UUID coachId);

    Optional<Workout> findByIdAndCoachIdAndDeletedAtIsNull(UUID id, UUID coachId);
}
