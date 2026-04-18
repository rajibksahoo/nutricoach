package com.nutricoach.library.repository;

import com.nutricoach.library.entity.WorkoutSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutSectionRepository extends JpaRepository<WorkoutSection, UUID> {

    List<WorkoutSection> findByCoachIdAndDeletedAtIsNullOrderByNameAsc(UUID coachId);

    Optional<WorkoutSection> findByIdAndCoachIdAndDeletedAtIsNull(UUID id, UUID coachId);
}
