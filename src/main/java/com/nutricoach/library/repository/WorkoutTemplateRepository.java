package com.nutricoach.library.repository;

import com.nutricoach.library.entity.WorkoutTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkoutTemplateRepository extends JpaRepository<WorkoutTemplate, UUID> {
    List<WorkoutTemplate> findAllByOrderByNameAsc();
}
