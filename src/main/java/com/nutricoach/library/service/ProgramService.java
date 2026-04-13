package com.nutricoach.library.service;

import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.library.dto.*;
import com.nutricoach.library.entity.Program;
import com.nutricoach.library.entity.ProgramDay;
import com.nutricoach.library.entity.Workout;
import com.nutricoach.library.mapper.ProgramMapper;
import com.nutricoach.library.repository.ProgramDayRepository;
import com.nutricoach.library.repository.ProgramRepository;
import com.nutricoach.library.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgramService {

    private final ProgramRepository programRepository;
    private final ProgramDayRepository programDayRepository;
    private final WorkoutRepository workoutRepository;
    private final ProgramMapper programMapper;

    @Transactional
    public ProgramSummaryResponse create(UUID coachId, CreateProgramRequest req) {
        Program p = Program.builder()
                .coachId(coachId)
                .name(req.name())
                .description(req.description())
                .durationDays(req.durationDays())
                .build();
        return programMapper.toSummary(programRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<ProgramSummaryResponse> list(UUID coachId) {
        return programRepository.findByCoachIdAndDeletedAtIsNullOrderByNameAsc(coachId)
                .stream().map(programMapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public ProgramResponse get(UUID id, UUID coachId) {
        Program p = require(id, coachId);
        List<ProgramDay> days = programDayRepository.findByProgramIdOrderByDayNumberAsc(id);
        List<UUID> workoutIds = days.stream().map(ProgramDay::getWorkoutId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<UUID, String> workoutNames = workoutIds.isEmpty() ? Map.of() :
                workoutRepository.findAllById(workoutIds).stream()
                        .collect(Collectors.toMap(Workout::getId, Workout::getName));
        List<ProgramDayResponse> dayResponses = days.stream()
                .map(d -> new ProgramDayResponse(
                        d.getId(), d.getDayNumber(), d.getWorkoutId(),
                        d.getWorkoutId() == null ? null : workoutNames.getOrDefault(d.getWorkoutId(), "Unknown"),
                        d.getNotes()))
                .toList();
        return new ProgramResponse(p.getId(), p.getName(), p.getDescription(),
                p.getDurationDays(), dayResponses, p.getCreatedAt(), p.getUpdatedAt());
    }

    @Transactional
    public ProgramSummaryResponse update(UUID id, UUID coachId, UpdateProgramRequest req) {
        Program p = require(id, coachId);
        if (StringUtils.hasText(req.name()))   p.setName(req.name());
        if (req.description() != null)         p.setDescription(req.description());
        if (req.durationDays() != null) {
            if (req.durationDays() < p.getDurationDays()) {
                boolean hasBeyond = programDayRepository.findByProgramIdOrderByDayNumberAsc(id).stream()
                        .anyMatch(d -> d.getDayNumber() > req.durationDays());
                if (hasBeyond) {
                    throw NutriCoachException.conflict(
                            "Cannot shrink: program has days assigned beyond new duration");
                }
            }
            p.setDurationDays(req.durationDays());
        }
        return programMapper.toSummary(programRepository.save(p));
    }

    @Transactional
    public void delete(UUID id, UUID coachId) {
        Program p = require(id, coachId);
        programDayRepository.deleteByProgramId(id);
        p.setDeletedAt(Instant.now());
        programRepository.save(p);
    }

    @Transactional
    public ProgramResponse setDay(UUID programId, UUID coachId, int dayNumber, SetProgramDayRequest req) {
        Program p = require(programId, coachId);
        if (dayNumber < 1 || dayNumber > p.getDurationDays()) {
            throw NutriCoachException.badRequest(
                    "Day number must be between 1 and " + p.getDurationDays());
        }
        if (req.workoutId() != null) {
            workoutRepository.findByIdAndCoachIdAndDeletedAtIsNull(req.workoutId(), coachId)
                    .orElseThrow(() -> NutriCoachException.notFound("Workout not found"));
        }
        ProgramDay day = programDayRepository.findByProgramIdAndDayNumber(programId, dayNumber)
                .orElseGet(() -> ProgramDay.builder()
                        .programId(programId)
                        .dayNumber(dayNumber)
                        .build());
        day.setWorkoutId(req.workoutId());
        day.setNotes(req.notes());
        programDayRepository.save(day);
        return get(programId, coachId);
    }

    @Transactional
    public ProgramResponse clearDay(UUID programId, UUID coachId, int dayNumber) {
        require(programId, coachId);
        programDayRepository.findByProgramIdAndDayNumber(programId, dayNumber)
                .ifPresent(programDayRepository::delete);
        return get(programId, coachId);
    }

    private Program require(UUID id, UUID coachId) {
        return programRepository.findByIdAndCoachIdAndDeletedAtIsNull(id, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Program not found"));
    }
}
