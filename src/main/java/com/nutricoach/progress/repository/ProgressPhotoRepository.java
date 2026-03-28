package com.nutricoach.progress.repository;

import com.nutricoach.progress.entity.ProgressPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgressPhotoRepository extends JpaRepository<ProgressPhoto, UUID> {

    List<ProgressPhoto> findByCoachIdAndProgressLogIdOrderByCreatedAtAsc(UUID coachId, UUID progressLogId);

    Optional<ProgressPhoto> findByIdAndCoachId(UUID id, UUID coachId);

    void deleteByIdAndCoachId(UUID id, UUID coachId);
}
