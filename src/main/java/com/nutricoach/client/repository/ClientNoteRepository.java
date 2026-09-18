package com.nutricoach.client.repository;

import com.nutricoach.client.entity.ClientNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientNoteRepository extends JpaRepository<ClientNote, UUID> {

    List<ClientNote> findByCoachIdAndClientIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID coachId, UUID clientId);

    Optional<ClientNote> findByIdAndCoachIdAndDeletedAtIsNull(UUID id, UUID coachId);
}
