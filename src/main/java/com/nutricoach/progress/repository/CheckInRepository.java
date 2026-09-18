package com.nutricoach.progress.repository;

import com.nutricoach.progress.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

    /**
     * Latest check-in date per client for one coach — dashboard overdue detection.
     * One query instead of one per client.
     */
    @Query("""
        SELECT c.clientId AS clientId, MAX(c.checkInDate) AS lastDate
        FROM CheckIn c
        WHERE c.coachId = :coachId AND c.deletedAt IS NULL
        GROUP BY c.clientId
        """)
    List<LastCheckIn> findLastCheckInPerClient(@Param("coachId") UUID coachId);

    /** Projection for {@link #findLastCheckInPerClient}. */
    interface LastCheckIn {
        UUID getClientId();
        LocalDate getLastDate();
    }

    /** All check-ins a coach received on a given date — dashboard "today" panel. */
    List<CheckIn> findByCoachIdAndCheckInDateAndDeletedAtIsNull(UUID coachId, LocalDate checkInDate);

    /** Newest check-ins across all clients — dashboard activity feed. */
    List<CheckIn> findTop15ByCoachIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID coachId);

    /** Newest check-ins for one client — the client-detail Updates feed. */
    List<CheckIn> findTop15ByCoachIdAndClientIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID coachId, UUID clientId);

    List<CheckIn> findByClientIdAndCoachIdAndDeletedAtIsNullOrderByCheckInDateDesc(UUID clientId, UUID coachId);

    Optional<CheckIn> findByIdAndCoachIdAndDeletedAtIsNull(UUID id, UUID coachId);

    Optional<CheckIn> findByClientIdAndCoachIdAndCheckInDate(UUID clientId, UUID coachId, LocalDate checkInDate);

    boolean existsByClientIdAndCoachIdAndCheckInDateAndDeletedAtIsNull(UUID clientId, UUID coachId, LocalDate checkInDate);

    boolean existsByClientIdAndCheckInDateAfterAndDeletedAtIsNull(UUID clientId, LocalDate date);
}
