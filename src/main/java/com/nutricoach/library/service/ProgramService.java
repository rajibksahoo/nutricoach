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
import com.nutricoach.progress.service.S3Service;
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
    private final S3Service s3Service;

    /** Cover gradients used as a fallback tile (mirrors the design palette). */
    private static final List<String> COVER_GRADIENTS = List.of(
            "linear-gradient(135deg,#4F46E5 0%,#7C3AED 100%)",
            "linear-gradient(135deg,#0D9488 0%,#0891B2 100%)",
            "linear-gradient(135deg,#F97316 0%,#DC2626 100%)",
            "linear-gradient(135deg,#1F2937 0%,#4B5563 100%)",
            "linear-gradient(135deg,#DB2777 0%,#F472B6 100%)",
            "linear-gradient(135deg,#0EA5E9 0%,#6366F1 100%)");

    @Transactional
    public ProgramSummaryResponse create(UUID coachId, CreateProgramRequest req) {
        int weeks = resolveWeeks(req.weeks(), req.durationDays());
        Program p = Program.builder()
                .coachId(coachId)
                .name(req.name())
                .description(req.description())
                .weeks(weeks)
                .durationDays(weeks * 7)
                .modality(req.modality())
                .experienceLevel(req.experienceLevel())
                .tags(req.tags())
                .coverGradient(gradientFor(req.name()))
                .build();
        return toSummary(programRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<ProgramSummaryResponse> list(UUID coachId) {
        return programRepository.findByCoachIdAndDeletedAtIsNullOrderByNameAsc(coachId)
                .stream().map(this::toSummary).toList();
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
                p.getDurationDays(), p.getWeeks(), p.getModality(), p.getExperienceLevel(),
                p.getTags(), coverUrl(p), p.getCoverGradient(),
                dayResponses, p.getCreatedAt(), p.getUpdatedAt());
    }

    @Transactional
    public ProgramSummaryResponse update(UUID id, UUID coachId, UpdateProgramRequest req) {
        Program p = require(id, coachId);
        if (StringUtils.hasText(req.name())) p.setName(req.name());
        if (req.description() != null)       p.setDescription(req.description());
        if (req.modality() != null)          p.setModality(req.modality());
        if (req.experienceLevel() != null)   p.setExperienceLevel(req.experienceLevel());
        if (req.tags() != null)              p.setTags(req.tags());

        Integer newDuration = null;
        if (req.weeks() != null) {
            newDuration = req.weeks() * 7;
        } else if (req.durationDays() != null) {
            newDuration = req.durationDays();
        }
        if (newDuration != null && newDuration != p.getDurationDays()) {
            if (newDuration < p.getDurationDays()) {
                final int limit = newDuration;
                boolean hasBeyond = programDayRepository.findByProgramIdOrderByDayNumberAsc(id).stream()
                        .anyMatch(d -> d.getDayNumber() > limit);
                if (hasBeyond) {
                    throw NutriCoachException.conflict(
                            "Cannot shrink: program has days assigned beyond new duration");
                }
            }
            p.setDurationDays(newDuration);
            p.setWeeks((int) Math.ceil(newDuration / 7.0));
        }
        return toSummary(programRepository.save(p));
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

    @Transactional
    public ProgramCoverUploadResponse initiateCoverUpload(UUID id, UUID coachId, ProgramCoverUploadRequest req) {
        Program p = require(id, coachId);
        if (p.getCoverS3Key() != null) {
            s3Service.deleteObject(p.getCoverS3Key());
        }
        String key = S3Service.programCoverKey(coachId, id);
        S3Service.PresignedUploadResult upload = s3Service.presignUpload(key, req.contentType());
        p.setCoverS3Key(upload.s3Key());
        programRepository.save(p);
        return new ProgramCoverUploadResponse(upload.uploadUrl(), upload.s3Key());
    }

    @Transactional
    public void deleteCover(UUID id, UUID coachId) {
        Program p = require(id, coachId);
        if (p.getCoverS3Key() != null) {
            s3Service.deleteObject(p.getCoverS3Key());
            p.setCoverS3Key(null);
            programRepository.save(p);
        }
    }

    private ProgramSummaryResponse toSummary(Program p) {
        return programMapper.toSummary(p, coverUrl(p));
    }

    private String coverUrl(Program p) {
        return p.getCoverS3Key() == null ? null : s3Service.presignDownload(p.getCoverS3Key());
    }

    private int resolveWeeks(Integer weeks, Integer durationDays) {
        if (weeks != null) return weeks;
        if (durationDays != null) return (int) Math.ceil(durationDays / 7.0);
        return 1;
    }

    private String gradientFor(String name) {
        int idx = Math.floorMod((name == null ? "" : name).hashCode(), COVER_GRADIENTS.size());
        return COVER_GRADIENTS.get(idx);
    }

    private Program require(UUID id, UUID coachId) {
        return programRepository.findByIdAndCoachIdAndDeletedAtIsNull(id, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Program not found"));
    }
}
