package com.nutricoach.library;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.library.entity.*;
import com.nutricoach.library.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Coach-owned program templates: promoting a program to a template, listing
 * templates apart from real programs, and starting a new program from one.
 */
class ProgramTemplateIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ProgramRepository programRepository;
    @Autowired ProgramDayRepository programDayRepository;
    @Autowired WorkoutRepository workoutRepository;
    @Autowired WorkoutSectionRepository sectionRepository;
    @Autowired WorkoutSectionAssignmentRepository sectionAssignmentRepository;
    @Autowired WorkoutSectionExerciseRepository sectionExerciseRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired JwtService jwtService;

    private static final String BASE = "/api/v1/library/programs";

    private String jwt;
    private Coach coach;
    private Coach rivalCoach;

    @BeforeEach
    void setup() {
        cleanup("9800080001");
        cleanup("9800080002");

        coach = coachRepository.save(Coach.builder()
                .phone("9800080001").name("Template Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)).build());
        rivalCoach = coachRepository.save(Coach.builder()
                .phone("9800080002").name("Rival Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)).build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    private void cleanup(String phone) {
        coachRepository.findByPhone(phone).ifPresent(existing -> {
            UUID id = existing.getId();
            sectionExerciseRepository.deleteAll(sectionExerciseRepository.findAll().stream()
                    .filter(se -> sectionRepository.findById(se.getSectionId())
                            .map(x -> x.getCoachId().equals(id)).orElse(false)).toList());
            sectionAssignmentRepository.deleteAll(sectionAssignmentRepository.findAll().stream()
                    .filter(sa -> workoutRepository.findById(sa.getWorkoutId())
                            .map(w -> w.getCoachId().equals(id)).orElse(false)).toList());
            programRepository.findAll().stream()
                    .filter(p -> p.getCoachId().equals(id))
                    .forEach(p -> {
                        programDayRepository.deleteAll(
                                programDayRepository.findByProgramIdOrderByDayNumberAsc(p.getId()));
                        programRepository.delete(p);
                    });
            sectionRepository.deleteAll(sectionRepository.findAll().stream()
                    .filter(x -> x.getCoachId().equals(id)).toList());
            workoutRepository.deleteAll(workoutRepository.findAll().stream()
                    .filter(w -> w.getCoachId().equals(id)).toList());
            exerciseRepository.deleteAll(exerciseRepository.findAll().stream()
                    .filter(e -> e.getCoachId().equals(id)).toList());
            coachRepository.delete(existing);
        });
    }

    /** A workout whose single exercise records the given equipment. */
    private Workout workoutUsing(String workoutName, String equipment) {
        Exercise exercise = exerciseRepository.save(Exercise.builder()
                .coachId(coach.getId()).name(workoutName + " lift").equipment(equipment).build());
        WorkoutSection section = sectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name("Main").sectionType(WorkoutSection.Type.MAIN).build());
        sectionExerciseRepository.save(WorkoutSectionExercise.builder()
                .sectionId(section.getId()).exerciseId(exercise.getId()).position(0).build());
        Workout workout = workoutRepository.save(Workout.builder()
                .coachId(coach.getId()).name(workoutName).build());
        sectionAssignmentRepository.save(WorkoutSectionAssignment.builder()
                .workoutId(workout.getId()).sectionId(section.getId()).position(0).build());
        return workout;
    }

    private Program program(String name, boolean isTemplate, UUID owner) {
        return programRepository.save(Program.builder()
                .coachId(owner).name(name).weeks(2).durationDays(14)
                .description("A blueprint").modality("Strength & Hypertrophy")
                .experienceLevel("Beginner").tags(List.of("Strength"))
                .isTemplate(isTemplate).build());
    }

    private void day(Program p, int dayNumber, Workout w, String notes) {
        programDayRepository.save(ProgramDay.builder()
                .programId(p.getId()).dayNumber(dayNumber).workoutId(w.getId()).notes(notes).build());
    }

    @Test
    void list_defaultsToBoth_andFiltersByTemplateFlag() throws Exception {
        program("Real Program", false, coach.getId());
        program("Blueprint", true, coach.getId());

        mockMvc.perform(get(BASE).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get(BASE).param("templates", "true").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Blueprint"))
                .andExpect(jsonPath("$.data[0].isTemplate").value(true));

        mockMvc.perform(get(BASE).param("templates", "false").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Real Program"));
    }

    @Test
    void setTemplate_promotesAndDemotes() throws Exception {
        Program p = program("Promote Me", false, coach.getId());

        mockMvc.perform(put(BASE + "/{id}/template", p.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("isTemplate", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isTemplate").value(true));

        mockMvc.perform(put(BASE + "/{id}/template", p.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("isTemplate", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isTemplate").value(false));
    }

    @Test
    void instantiate_copiesDaysAndNotes_andTheCopyIsNotATemplate() throws Exception {
        Program template = program("8-Week Base", true, coach.getId());
        day(template, 1, workoutUsing("Push", "Barbell"), "Keep RPE 7");
        day(template, 4, workoutUsing("Pull", "Dumbbell"), null);

        String body = mockMvc.perform(post(BASE + "/{id}/instantiate", template.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Arjun — Block 1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Arjun — Block 1"))
                .andExpect(jsonPath("$.data.isTemplate").value(false))
                .andExpect(jsonPath("$.data.weeks").value(2))
                // Equipment is a native query run right after the days are saved;
                // if the copy's days were not flushed first this comes back empty.
                .andExpect(jsonPath("$.data.equipment.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        UUID copyId = UUID.fromString(objectMapper.readTree(body).path("data").path("id").asText());
        List<ProgramDay> copied = programDayRepository.findByProgramIdOrderByDayNumberAsc(copyId);

        assertThat(copied).hasSize(2);
        assertThat(copied.get(0).getDayNumber()).isEqualTo(1);
        assertThat(copied.get(0).getNotes()).isEqualTo("Keep RPE 7");
        assertThat(copied.get(1).getDayNumber()).isEqualTo(4);
        // The source template keeps its own days — this is a copy, not a move.
        assertThat(programDayRepository.findByProgramIdOrderByDayNumberAsc(template.getId())).hasSize(2);
    }

    @Test
    void instantiate_withoutAName_suffixesTheSource() throws Exception {
        Program template = program("Mobility Base", true, coach.getId());

        mockMvc.perform(post(BASE + "/{id}/instantiate", template.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Mobility Base (copy)"));
    }

    @Test
    void instantiate_anotherCoachsProgram_returns404() throws Exception {
        Program theirs = program("Not Yours", true, rivalCoach.getId());

        mockMvc.perform(post(BASE + "/{id}/instantiate", theirs.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void setTemplate_onAnotherCoachsProgram_returns404() throws Exception {
        Program theirs = program("Not Yours", false, rivalCoach.getId());

        mockMvc.perform(put(BASE + "/{id}/template", theirs.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("isTemplate", true))))
                .andExpect(status().isNotFound());
    }

    @Test
    void instantiate_withoutToken_returns401() throws Exception {
        Program template = program("Blueprint", true, coach.getId());

        mockMvc.perform(post(BASE + "/{id}/instantiate", template.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
