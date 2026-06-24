package com.nutricoach.library.repository;

import com.nutricoach.library.entity.ClientProgramAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientProgramAssignmentRepository extends JpaRepository<ClientProgramAssignment, UUID> {

    List<ClientProgramAssignment> findByProgramIdAndDeletedAtIsNull(UUID programId);

    List<ClientProgramAssignment> findByCoachIdAndClientIdAndDeletedAtIsNullOrderByAssignedAtDesc(UUID coachId, UUID clientId);

    Optional<ClientProgramAssignment> findByIdAndCoachIdAndDeletedAtIsNull(UUID id, UUID coachId);

    Optional<ClientProgramAssignment> findByClientIdAndProgramIdAndDeletedAtIsNull(UUID clientId, UUID programId);
}
