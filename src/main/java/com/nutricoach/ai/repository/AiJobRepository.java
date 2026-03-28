package com.nutricoach.ai.repository;

import com.nutricoach.ai.entity.AiJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiJobRepository extends JpaRepository<AiJob, UUID> {

    List<AiJob> findByCoachIdOrderByCreatedAtDesc(UUID coachId);

    Optional<AiJob> findByIdAndCoachId(UUID id, UUID coachId);
}
