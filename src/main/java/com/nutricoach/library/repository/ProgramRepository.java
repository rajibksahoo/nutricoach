package com.nutricoach.library.repository;

import com.nutricoach.library.entity.Program;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramRepository extends JpaRepository<Program, UUID> {

    List<Program> findByCoachIdAndDeletedAtIsNullOrderByNameAsc(UUID coachId);

    Optional<Program> findByIdAndCoachIdAndDeletedAtIsNull(UUID id, UUID coachId);
}
