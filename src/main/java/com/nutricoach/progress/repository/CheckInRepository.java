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
        WHERE c.coachId = :coachId
        GROUP BY c.clientId
        """)
    List<LastCheckIn> findLastCheckInPerClient(@Param("coachId") UUID coachId);

    /** Projection for {@link #findLastCheckInPerClient}. */
    interface LastCheckIn {
        UUID getClientId();
        LocalDate getLastDate();
    }

    /** All check-ins a coach received on a given date — dashboard "today" panel. */
    List<CheckIn> findByCoachIdAndCheckInDate(UUID coachId, LocalDate checkInDate);

    /** Newest check-ins across all clients — dashboard activity feed. */
    List<CheckIn> findTop15ByCoachIdOrderByCreatedAtDesc(UUID coachId);

    List<CheckIn> findByClientIdAndCoachIdOrderByCheckInDateDesc(UUID clientId, UUID coachId);

    Optional<CheckIn> findByClientIdAndCoachIdAndCheckInDate(UUID clientId, UUID coachId, LocalDate checkInDate);

    boolean existsByClientIdAndCoachIdAndCheckInDate(UUID clientId, UUID coachId, LocalDate checkInDate);

    boolean existsByClientIdAndCheckInDateAfter(UUID clientId, LocalDate date);
}
