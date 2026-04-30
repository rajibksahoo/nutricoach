package com.nutricoach.library.service;

import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.library.dto.WorkoutResponse;
import com.nutricoach.library.dto.WorkoutTemplateResponse;
import com.nutricoach.library.entity.*;
import com.nutricoach.library.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkoutTemplateService {

    private final WorkoutTemplateRepository templateRepository;
    private final WorkoutRepository workoutRepository;
    private final WorkoutSectionRepository sectionRepository;
    private final WorkoutSectionExerciseRepository sectionExerciseRepository;
    private final WorkoutSectionAssignmentRepository assignmentRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutService workoutService;

    @Transactional(readOnly = true)
    public List<WorkoutTemplateResponse> list() {
        return templateRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public WorkoutTemplateResponse get(UUID id) {
        return toResponse(requireTemplate(id));
    }

    @Transactional
    public WorkoutResponse instantiate(UUID templateId, UUID coachId) {
        WorkoutTemplate template = requireTemplate(templateId);

        Workout workout = workoutRepository.save(Workout.builder()
                .coachId(coachId)
                .name(template.getName())
                .description(template.getDescription())
                .tags(template.getEquipment())
                .build());

        Map<String, Exercise> matchCache = new HashMap<>();
        List<Exercise> coachLibrary = exerciseRepository.findByCoachIdAndDeletedAtIsNullOrderByNameAsc(coachId);
        Map<String, Exercise> byName = new HashMap<>();
        for (Exercise ex : coachLibrary) byName.put(ex.getName().trim().toLowerCase(), ex);

        int sectionPos = 0;
        for (WorkoutTemplate.Section ts : template.getSections()) {
            WorkoutSection section = sectionRepository.save(WorkoutSection.builder()
                    .coachId(coachId)
                    .name(ts.getTitle())
                    .sectionType(inferSectionType(ts.getTitle()))
                    .description(ts.getStyle())
                    .build());

            int itemPos = 0;
            for (WorkoutTemplate.Item item : ts.getItems()) {
                Exercise exercise = matchCache.computeIfAbsent(item.getName(), key -> {
                    Exercise found = byName.get(key.trim().toLowerCase());
                    if (found != null) return found;
                    Exercise created = exerciseRepository.save(Exercise.builder()
                            .coachId(coachId)
                            .name(item.getName())
                            .category(mapThumbToCategory(item.getThumb()))
                            .custom(true)
                            .build());
                    byName.put(key.trim().toLowerCase(), created);
                    return created;
                });

                sectionExerciseRepository.save(WorkoutSectionExercise.builder()
                        .sectionId(section.getId())
                        .exerciseId(exercise.getId())
                        .position(itemPos++)
                        .notes(item.getReps() + (item.getNote() != null && !item.getNote().isBlank() ? " · " + item.getNote() : ""))
                        .build());
            }

            assignmentRepository.save(WorkoutSectionAssignment.builder()
                    .workoutId(workout.getId())
                    .sectionId(section.getId())
                    .position(sectionPos++)
                    .build());
        }

        return workoutService.get(workout.getId(), coachId);
    }

    private WorkoutTemplate requireTemplate(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> NutriCoachException.notFound("Workout template not found"));
    }

    private WorkoutTemplateResponse toResponse(WorkoutTemplate t) {
        return new WorkoutTemplateResponse(
                t.getId(), t.getName(), t.getDescription(),
                t.getCoverGradient(), t.getEquipment(), t.getSections());
    }

    private WorkoutSection.Type inferSectionType(String title) {
        String t = title == null ? "" : title.toLowerCase();
        if (t.contains("warm")) return WorkoutSection.Type.WARM_UP;
        if (t.contains("cool") || t.contains("stretch")) return WorkoutSection.Type.COOL_DOWN;
        if (t.contains("finish")) return WorkoutSection.Type.FINISHER;
        if (t.contains("accessor")) return WorkoutSection.Type.ACCESSORY;
        return WorkoutSection.Type.MAIN;
    }

    private Exercise.Category mapThumbToCategory(String thumb) {
        if (thumb == null) return Exercise.Category.strength;
        return switch (thumb.toLowerCase()) {
            case "bodyweight" -> Exercise.Category.bodyweight;
            case "timed" -> Exercise.Category.timed;
            case "cardio" -> Exercise.Category.cardio;
            case "amrap" -> Exercise.Category.amrap;
            default -> Exercise.Category.strength;
        };
    }
}
