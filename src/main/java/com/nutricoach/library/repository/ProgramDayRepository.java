package com.nutricoach.library.repository;

import com.nutricoach.library.entity.ProgramDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramDayRepository extends JpaRepository<ProgramDay, UUID> {

    List<ProgramDay> findByProgramIdOrderByDayNumberAsc(UUID programId);

    Optional<ProgramDay> findByProgramIdAndDayNumber(UUID programId, int dayNumber);

    boolean existsByWorkoutId(UUID workoutId);

    void deleteByProgramId(UUID programId);
}
