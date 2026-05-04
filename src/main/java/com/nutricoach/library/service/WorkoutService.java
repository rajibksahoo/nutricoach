package com.nutricoach.library.service;

import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.library.dto.*;
import com.nutricoach.library.entity.*;
import com.nutricoach.library.mapper.WorkoutMapper;
import com.nutricoach.library.repository.*;
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
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final WorkoutSectionRepository sectionRepository;
    private final WorkoutSectionExerciseRepository sectionExerciseRepository;
    private final WorkoutSectionAssignmentRepository assignmentRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutMapper workoutMapper;

    // ── Workouts ──────────────────────────────────────────────────────────────

    @Transactional
    public WorkoutSummaryResponse create(UUID coachId, CreateWorkoutRequest req) {
        Workout w = Workout.builder()
                .coachId(coachId)
                .name(req.name())
                .description(req.description())
                .estimatedDurationMinutes(req.estimatedDurationMinutes())
                .tags(req.tags())
                .build();
        return workoutMapper.toSummary(workoutRepository.save(w));
    }

    @Transactional(readOnly = true)
    public List<WorkoutSummaryResponse> list(UUID coachId) {
        return workoutRepository.findByCoachIdAndDeletedAtIsNullOrderByNameAsc(coachId)
                .stream().map(workoutMapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public WorkoutResponse get(UUID id, UUID coachId) {
        Workout w = requireWorkout(id, coachId);
        List<WorkoutSectionAssignment> assignments = assignmentRepository.findByWorkoutIdOrderByPositionAsc(id);
        List<WorkoutSectionResponse> sectionResponses = assignments.stream()
                .map(a -> {
                    WorkoutSectionResponse r = buildSectionResponseOrNull(a.getSectionId(), coachId);
                    return r == null ? null : new WorkoutSectionResponse(
                            r.id(), a.getId(), r.name(), r.sectionType(), r.description(),
                            r.exercises(), r.createdAt(), r.updatedAt());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
        return new WorkoutResponse(
                w.getId(), w.getName(), w.getDescription(), w.getEstimatedDurationMinutes(),
                w.getTags(), sectionResponses, w.getCreatedAt(), w.getUpdatedAt());
    }

    @Transactional
    public WorkoutSummaryResponse update(UUID id, UUID coachId, UpdateWorkoutRequest req) {
        Workout w = requireWorkout(id, coachId);
        if (StringUtils.hasText(req.name()))               w.setName(req.name());
        if (req.description() != null)                     w.setDescription(req.description());
        if (req.estimatedDurationMinutes() != null)        w.setEstimatedDurationMinutes(req.estimatedDurationMinutes());
        if (req.tags() != null)                            w.setTags(req.tags());
        return workoutMapper.toSummary(workoutRepository.save(w));
    }

    @Transactional
    public WorkoutResponse duplicate(UUID id, UUID coachId) {
        Workout original = requireWorkout(id, coachId);
        Workout copy = workoutRepository.save(Workout.builder()
                .coachId(coachId)
                .name(original.getName() + " (copy)")
                .description(original.getDescription())
                .estimatedDurationMinutes(original.getEstimatedDurationMinutes())
                .tags(original.getTags())
                .build());

        List<WorkoutSectionAssignment> originals = assignmentRepository.findByWorkoutIdOrderByPositionAsc(id);
        for (WorkoutSectionAssignment a : originals) {
            assignmentRepository.save(WorkoutSectionAssignment.builder()
                    .workoutId(copy.getId())
                    .sectionId(a.getSectionId())
                    .position(a.getPosition())
                    .build());
        }
        return get(copy.getId(), coachId);
    }

    @Transactional
    public void delete(UUID id, UUID coachId) {
        Workout w = requireWorkout(id, coachId);
        assignmentRepository.deleteByWorkoutId(id);
        w.setDeletedAt(Instant.now());
        workoutRepository.save(w);
    }

    // ── Workout → Sections (attach / detach) ──────────────────────────────────

    @Transactional
    public WorkoutResponse attachSection(UUID workoutId, UUID coachId, AttachSectionRequest req) {
        requireWorkout(workoutId, coachId);
        WorkoutSection section = sectionRepository.findByIdAndCoachIdAndDeletedAtIsNull(req.sectionId(), coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Workout section not found"));

        int position = req.position() != null ? req.position()
                : assignmentRepository.findByWorkoutIdOrderByPositionAsc(workoutId).size();

        assignmentRepository.save(WorkoutSectionAssignment.builder()
                .workoutId(workoutId)
                .sectionId(section.getId())
                .position(position)
                .build());

        return get(workoutId, coachId);
    }

    @Transactional
    public void detachSection(UUID workoutId, UUID coachId, UUID assignmentId) {
        requireWorkout(workoutId, coachId);
        WorkoutSectionAssignment a = assignmentRepository.findByIdAndWorkoutId(assignmentId, workoutId)
                .orElseThrow(() -> NutriCoachException.notFound("Section assignment not found"));
        assignmentRepository.delete(a);
    }

    // ── Sections (standalone CRUD) ────────────────────────────────────────────

    @Transactional
    public WorkoutSectionResponse createSection(UUID coachId, CreateWorkoutSectionRequest req) {
        WorkoutSection s = WorkoutSection.builder()
                .coachId(coachId)
                .name(req.name())
                .sectionType(req.sectionType())
                .description(req.description())
                .build();
        s = sectionRepository.save(s);
        return buildSectionResponse(s, List.of(), Map.of());
    }

    @Transactional(readOnly = true)
    public List<WorkoutSectionResponse> listSections(UUID coachId) {
        return sectionRepository.findByCoachIdAndDeletedAtIsNullOrderByNameAsc(coachId).stream()
                .map(s -> buildSectionResponseOrNull(s.getId(), coachId))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkoutSectionResponse getSection(UUID id, UUID coachId) {
        WorkoutSectionResponse r = buildSectionResponseOrNull(id, coachId);
        if (r == null) throw NutriCoachException.notFound("Workout section not found");
        return r;
    }

    @Transactional
    public WorkoutSectionResponse updateSection(UUID id, UUID coachId, UpdateWorkoutSectionRequest req) {
        WorkoutSection s = requireSection(id, coachId);
        if (StringUtils.hasText(req.name()))    s.setName(req.name());
        if (req.sectionType() != null)          s.setSectionType(req.sectionType());
        if (req.description() != null)          s.setDescription(req.description());
        sectionRepository.save(s);
        return getSection(id, coachId);
    }

    @Transactional
    public void deleteSection(UUID id, UUID coachId) {
        WorkoutSection s = requireSection(id, coachId);
        if (assignmentRepository.existsBySectionId(id)) {
            throw NutriCoachException.conflict("Section is used by one or more workouts");
        }
        sectionExerciseRepository.deleteBySectionId(id);
        s.setDeletedAt(Instant.now());
        sectionRepository.save(s);
    }

    // ── Sections → Exercises ──────────────────────────────────────────────────

    @Transactional
    public WorkoutSectionResponse addSectionExercise(UUID sectionId, UUID coachId, AddSectionExerciseRequest req) {
        requireSection(sectionId, coachId);
        exerciseRepository.findByIdAndCoachIdAndDeletedAtIsNull(req.exerciseId(), coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Exercise not found"));

        int position = req.position() != null ? req.position()
                : sectionExerciseRepository.findBySectionIdOrderByPositionAsc(sectionId).size();

        sectionExerciseRepository.save(WorkoutSectionExercise.builder()
                .sectionId(sectionId)
                .exerciseId(req.exerciseId())
                .position(position)
                .sets(req.sets())
                .reps(req.reps())
                .durationSeconds(req.durationSeconds())
                .restSeconds(req.restSeconds())
                .weight(req.weight())
                .notes(req.notes())
                .build());

        return getSection(sectionId, coachId);
    }

    @Transactional
    public WorkoutSectionResponse updateSectionExercise(UUID sectionId, UUID coachId, UUID entryId,
                                                        PatchSectionExerciseRequest req) {
        requireSection(sectionId, coachId);
        WorkoutSectionExercise entry = sectionExerciseRepository.findByIdAndSectionId(entryId, sectionId)
                .orElseThrow(() -> NutriCoachException.notFound("Section exercise not found"));
        if (req.position() != null)         entry.setPosition(req.position());
        if (req.sets() != null)             entry.setSets(req.sets());
        if (req.reps() != null)             entry.setReps(req.reps());
        if (req.durationSeconds() != null)  entry.setDurationSeconds(req.durationSeconds());
        if (req.restSeconds() != null)      entry.setRestSeconds(req.restSeconds());
        if (req.weight() != null)           entry.setWeight(req.weight());
        if (req.notes() != null)            entry.setNotes(req.notes());
        sectionExerciseRepository.save(entry);
        return getSection(sectionId, coachId);
    }

    @Transactional
    public WorkoutSectionResponse reorderSectionExercises(UUID sectionId, UUID coachId, ReorderRequest req) {
        requireSection(sectionId, coachId);
        List<WorkoutSectionExercise> entries = sectionExerciseRepository.findBySectionIdOrderByPositionAsc(sectionId);
        Map<UUID, WorkoutSectionExercise> byId = entries.stream()
                .collect(Collectors.toMap(WorkoutSectionExercise::getId, e -> e));
        if (req.orderedIds().size() != entries.size() || !byId.keySet().containsAll(req.orderedIds())) {
            throw NutriCoachException.badRequest("orderedIds must contain every entry id in this section exactly once");
        }
        for (int i = 0; i < req.orderedIds().size(); i++) {
            byId.get(req.orderedIds().get(i)).setPosition(i);
        }
        sectionExerciseRepository.saveAll(entries);
        return getSection(sectionId, coachId);
    }

    @Transactional
    public WorkoutResponse reorderWorkoutSections(UUID workoutId, UUID coachId, ReorderRequest req) {
        requireWorkout(workoutId, coachId);
        List<WorkoutSectionAssignment> assignments = assignmentRepository.findByWorkoutIdOrderByPositionAsc(workoutId);
        Map<UUID, WorkoutSectionAssignment> byId = assignments.stream()
                .collect(Collectors.toMap(WorkoutSectionAssignment::getId, a -> a));
        if (req.orderedIds().size() != assignments.size() || !byId.keySet().containsAll(req.orderedIds())) {
            throw NutriCoachException.badRequest("orderedIds must contain every assignment id in this workout exactly once");
        }
        for (int i = 0; i < req.orderedIds().size(); i++) {
            byId.get(req.orderedIds().get(i)).setPosition(i);
        }
        assignmentRepository.saveAll(assignments);
        return get(workoutId, coachId);
    }

    @Transactional
    public void removeSectionExercise(UUID sectionId, UUID coachId, UUID entryId) {
        requireSection(sectionId, coachId);
        WorkoutSectionExercise entry = sectionExerciseRepository.findByIdAndSectionId(entryId, sectionId)
                .orElseThrow(() -> NutriCoachException.notFound("Section exercise not found"));
        sectionExerciseRepository.delete(entry);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private WorkoutSectionResponse buildSectionResponseOrNull(UUID sectionId, UUID coachId) {
        return sectionRepository.findByIdAndCoachIdAndDeletedAtIsNull(sectionId, coachId)
                .map(s -> {
                    List<WorkoutSectionExercise> entries = sectionExerciseRepository.findBySectionIdOrderByPositionAsc(s.getId());
                    List<UUID> exerciseIds = entries.stream().map(WorkoutSectionExercise::getExerciseId).distinct().toList();
                    Map<UUID, String> exerciseNames = exerciseIds.isEmpty() ? Map.of() :
                            exerciseRepository.findAllById(exerciseIds).stream()
                                    .collect(Collectors.toMap(Exercise::getId, Exercise::getName));
                    return buildSectionResponse(s, entries, exerciseNames);
                })
                .orElse(null);
    }

    private WorkoutSectionResponse buildSectionResponse(WorkoutSection s,
                                                        List<WorkoutSectionExercise> entries,
                                                        Map<UUID, String> exerciseNames) {
        List<SectionExerciseResponse> items = entries.stream()
                .map(e -> new SectionExerciseResponse(
                        e.getId(), e.getExerciseId(),
                        exerciseNames.getOrDefault(e.getExerciseId(), "Unknown"),
                        e.getPosition(), e.getSets(), e.getReps(),
                        e.getDurationSeconds(), e.getRestSeconds(), e.getWeight(), e.getNotes()))
                .toList();
        return new WorkoutSectionResponse(s.getId(), null, s.getName(), s.getSectionType(),
                s.getDescription(), items, s.getCreatedAt(), s.getUpdatedAt());
    }

    private Workout requireWorkout(UUID id, UUID coachId) {
        return workoutRepository.findByIdAndCoachIdAndDeletedAtIsNull(id, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Workout not found"));
    }

    private WorkoutSection requireSection(UUID id, UUID coachId) {
        return sectionRepository.findByIdAndCoachIdAndDeletedAtIsNull(id, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Workout section not found"));
    }
}
