package com.nutricoach.library.service;

import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.library.dto.CreateExerciseRequest;
import com.nutricoach.library.dto.ExerciseResponse;
import com.nutricoach.library.dto.UpdateExerciseRequest;
import com.nutricoach.library.entity.Exercise;
import com.nutricoach.library.mapper.ExerciseMapper;
import com.nutricoach.library.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseMapper exerciseMapper;

    @Transactional
    public ExerciseResponse create(UUID coachId, CreateExerciseRequest req) {
        Exercise ex = Exercise.builder()
                .coachId(coachId)
                .name(req.name())
                .description(req.description())
                .muscleGroup(req.muscleGroup())
                .equipment(req.equipment())
                .videoUrl(req.videoUrl())
                .notes(req.notes())
                .build();
        return exerciseMapper.toResponse(exerciseRepository.save(ex));
    }

    @Transactional(readOnly = true)
    public List<ExerciseResponse> list(UUID coachId) {
        return exerciseRepository.findByCoachIdAndDeletedAtIsNullOrderByNameAsc(coachId)
                .stream().map(exerciseMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ExerciseResponse get(UUID id, UUID coachId) {
        return exerciseMapper.toResponse(require(id, coachId));
    }

    @Transactional
    public ExerciseResponse update(UUID id, UUID coachId, UpdateExerciseRequest req) {
        Exercise ex = require(id, coachId);
        if (StringUtils.hasText(req.name()))        ex.setName(req.name());
        if (req.description() != null)              ex.setDescription(req.description());
        if (req.muscleGroup() != null)              ex.setMuscleGroup(req.muscleGroup());
        if (req.equipment() != null)                ex.setEquipment(req.equipment());
        if (req.videoUrl() != null)                 ex.setVideoUrl(req.videoUrl());
        if (req.notes() != null)                    ex.setNotes(req.notes());
        return exerciseMapper.toResponse(exerciseRepository.save(ex));
    }

    @Transactional
    public void delete(UUID id, UUID coachId) {
        Exercise ex = require(id, coachId);
        ex.setDeletedAt(Instant.now());
        exerciseRepository.save(ex);
    }

    Exercise require(UUID id, UUID coachId) {
        return exerciseRepository.findByIdAndCoachIdAndDeletedAtIsNull(id, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Exercise not found"));
    }
}
