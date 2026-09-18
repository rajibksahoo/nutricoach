package com.nutricoach.library.repository;

import com.nutricoach.library.entity.ProgramDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramDayRepository extends JpaRepository<ProgramDay, UUID> {

    List<ProgramDay> findByProgramIdOrderByDayNumberAsc(UUID programId);

    Optional<ProgramDay> findByProgramIdAndDayNumber(UUID programId, int dayNumber);

    boolean existsByWorkoutId(UUID workoutId);

    void deleteByProgramId(UUID programId);

    /**
     * Distinct equipment used by each program, derived from the exercises inside
     * the workouts placed on its days.
     *
     * <p>Native because the hop from program to exercise crosses five tables and
     * none of them are mapped as JPA associations. One query covers every
     * program in the list, so the Programs screen does not fan out per row.
     *
     * @return rows of {@code [program_id (UUID), equipment (String)]}
     */
    @Query(value = """
            SELECT pd.program_id, e.equipment
            FROM program_days pd
            JOIN workouts w ON w.id = pd.workout_id AND w.deleted_at IS NULL
            JOIN workout_section_assignments wsa ON wsa.workout_id = w.id
            JOIN workout_sections ws ON ws.id = wsa.section_id AND ws.deleted_at IS NULL
            JOIN workout_section_exercises wse ON wse.section_id = ws.id
            JOIN exercises e ON e.id = wse.exercise_id AND e.deleted_at IS NULL
            WHERE pd.program_id IN (:programIds)
              AND e.equipment IS NOT NULL
              AND btrim(e.equipment) <> ''
            GROUP BY pd.program_id, e.equipment
            ORDER BY e.equipment
            """, nativeQuery = true)
    List<Object[]> findEquipmentByProgramIds(@Param("programIds") Collection<UUID> programIds);
}
