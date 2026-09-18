package com.nutricoach.progress.repository;

import com.nutricoach.progress.entity.ProgressPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgressPhotoRepository extends JpaRepository<ProgressPhoto, UUID> {

    /**
     * Every photo for one client, newest log first.
     *
     * <p>A {@link ProgressPhoto} belongs to a progress log, not directly to a
     * client, so this joins through {@code ProgressLog} to filter by client and
     * to carry each photo's log date back for the Progress Photos grid.
     *
     * @return rows of {@code [ProgressPhoto, LocalDate loggedDate]}
     */
    @Query("""
            SELECT p, l.loggedDate
            FROM ProgressPhoto p, ProgressLog l
            WHERE p.progressLogId = l.id
              AND l.clientId = :clientId
              AND p.coachId = :coachId
            ORDER BY l.loggedDate DESC, p.createdAt ASC
            """)
    List<Object[]> findByClientIdWithLogDate(@Param("clientId") UUID clientId, @Param("coachId") UUID coachId);

    List<ProgressPhoto> findByCoachIdAndProgressLogIdOrderByCreatedAtAsc(UUID coachId, UUID progressLogId);

    Optional<ProgressPhoto> findByIdAndCoachId(UUID id, UUID coachId);

    void deleteByIdAndCoachId(UUID id, UUID coachId);
}
