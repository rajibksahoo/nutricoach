package com.nutricoach.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.ai.entity.AiJob;
import com.nutricoach.ai.repository.AiJobRepository;
import com.nutricoach.ai.service.AiMealPlanService;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.plans.entity.MealPlan;
import com.nutricoach.plans.repository.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AI meal-plan generation, end to end against the local stub response
 * (`openai.api-key` starts with "local-" in application-test.yml, so no tokens
 * are spent and the parser is still fully exercised).
 *
 * <p>The behaviour under test is the part that was missing: the generated days,
 * meals and items must actually be persisted. Generation used to parse only the
 * plan name and save an empty DRAFT, so a test that asserted the job reached
 * COMPLETED — or that a plan row existed — would have passed against the bug.
 * Every assertion here is about plan *contents*.
 */
class AiMealPlanIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired AiJobRepository aiJobRepository;
    @Autowired AiMealPlanService aiMealPlanService;
    @Autowired MealPlanRepository mealPlanRepository;
    @Autowired MealPlanDayRepository dayRepository;
    @Autowired MealRepository mealRepository;
    @Autowired MealItemRepository mealItemRepository;
    @Autowired JwtService jwtService;

    private String jwt;
    private Coach coach;
    private Client client;
    private Client rivalClient;

    @BeforeEach
    void setup() {
        cleanup("9820000001");
        cleanup("9820000002");

        coach = coachRepository.save(Coach.builder()
                .phone("9820000001").name("AI Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)).build());
        Coach rivalCoach = coachRepository.save(Coach.builder()
                .phone("9820000002").name("Rival Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)).build());

        client = clientRepository.save(Client.builder()
                .coachId(coach.getId()).name("AI Client").phone("9821110001")
                .goal(Client.Goal.WEIGHT_LOSS).status(Client.Status.ACTIVE).build());
        rivalClient = clientRepository.save(Client.builder()
                .coachId(rivalCoach.getId()).name("Rival Client").phone("9821110002")
                .status(Client.Status.ACTIVE).build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    private void cleanup(String phone) {
        coachRepository.findByPhone(phone).ifPresent(existing -> {
            UUID id = existing.getId();
            aiJobRepository.deleteAll(aiJobRepository.findAll().stream()
                    .filter(j -> j.getCoachId().equals(id)).toList());
            mealPlanRepository.findAll().stream()
                    .filter(p -> p.getCoachId().equals(id))
                    .forEach(plan -> {
                        dayRepository.findByMealPlanIdOrderByDayNumber(plan.getId()).forEach(day -> {
                            mealRepository.findByMealPlanDayIdOrderBySequenceOrder(day.getId())
                                    .forEach(meal -> mealItemRepository
                                            .deleteAll(mealItemRepository.findByMealId(meal.getId())));
                            mealRepository.deleteAll(
                                    mealRepository.findByMealPlanDayIdOrderBySequenceOrder(day.getId()));
                            dayRepository.delete(day);
                        });
                        mealPlanRepository.delete(plan);
                    });
            clientRepository.deleteAll(clientRepository.findAllByCoachId(id));
            coachRepository.delete(existing);
        });
    }

    /**
     * processJob is {@code @Async}, so it returns before the work is done and
     * runs on another thread. Poll for a terminal state rather than asserting
     * immediately, which reports PENDING and looks like a parser failure.
     */
    private AiJob awaitJob(UUID jobId) {
        for (int i = 0; i < 100; i++) {
            AiJob job = aiJobRepository.findById(jobId).orElseThrow();
            if (job.getStatus() == AiJob.Status.COMPLETED || job.getStatus() == AiJob.Status.FAILED) {
                return job;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError("AI job did not finish within 10s");
    }

    private MealPlan generate() {
        AiJob created = aiMealPlanService.createJob(coach.getId(), client.getId());
        aiMealPlanService.processJob(created.getId());
        AiJob done = awaitJob(created.getId());
        assertThat(done.getStatus())
                .withFailMessage("job failed: %s", done.getErrorMessage())
                .isEqualTo(AiJob.Status.COMPLETED);
        UUID planId = UUID.fromString(done.getOutputPayload().get("mealPlanId").toString());
        return mealPlanRepository.findById(planId).orElseThrow();
    }

    @Test
    void generate_persistsDaysMealsAndItems() {
        MealPlan plan = generate();

        assertThat(plan.isAiGenerated()).isTrue();
        assertThat(plan.getStatus()).isEqualTo(MealPlan.Status.DRAFT);

        var days = dayRepository.findByMealPlanIdOrderByDayNumber(plan.getId());
        assertThat(days).hasSize(3);

        int meals = 0, items = 0;
        for (var day : days) {
            var dayMeals = mealRepository.findByMealPlanDayIdOrderBySequenceOrder(day.getId());
            meals += dayMeals.size();
            for (var meal : dayMeals) {
                items += mealItemRepository.findByMealId(meal.getId()).size();
            }
        }
        // The stub is 3 days x 4 meals, with one day carrying a two-item lunch.
        assertThat(meals).isEqualTo(12);
        assertThat(items).isEqualTo(13);
    }

    @Test
    void generate_recalculatesDayTotals() {
        MealPlan plan = generate();

        var days = dayRepository.findByMealPlanIdOrderByDayNumber(plan.getId());
        assertThat(days).allSatisfy(day -> {
            assertThat(day.getTotalCalories()).isNotNull().isPositive();
            assertThat(day.getTotalProteinG()).isNotNull();
        });
    }

    /**
     * A name the curated list holds under a qualifier ("Idli (steamed)") must
     * still resolve, otherwise almost every generated item would be treated as
     * unknown and our own macro data would go unused.
     */
    @Test
    void generate_matchesCuratedFoodsThroughTheirQualifiedNames() {
        MealPlan plan = generate();

        List<com.nutricoach.plans.entity.MealItem> all = dayRepository
                .findByMealPlanIdOrderByDayNumber(plan.getId()).stream()
                .flatMap(d -> mealRepository.findByMealPlanDayIdOrderBySequenceOrder(d.getId()).stream())
                .flatMap(m -> mealItemRepository.findByMealId(m.getId()).stream())
                .toList();

        assertThat(all).anyMatch(i -> i.getFoodItemId() != null)
                .withFailMessage("expected at least one item matched to a curated food");
        assertThat(all).anyMatch(i -> i.getFoodItemId() == null && i.getCustomName() != null)
                .withFailMessage("expected at least one item to fall back to a custom name");
        // Every item lands one way or the other — never both, never neither.
        assertThat(all).allSatisfy(i ->
                assertThat(i.getFoodItemId() == null).isEqualTo(i.getCustomName() != null));
    }

    /**
     * "Poha" is ambiguous: the curated list holds it cooked (130 kcal/100g) and
     * dry (356). A meal plan means the cooked form, and the choice must not
     * depend on row order.
     */
    @Test
    void generate_resolvesAnAmbiguousAliasToTheCookedForm() throws Exception {
        MealPlan plan = generate();

        mockMvc.perform(get("/api/v1/meal-plans/{id}", plan.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[0].meals[0].items[0].foodItemName")
                        .value("Poha (cooked)"))
                .andExpect(jsonPath("$.data.days[0].meals[0].items[0].custom").value(false));
    }

    @Test
    void generate_reportsCountsOnTheJob() {
        AiJob created = aiMealPlanService.createJob(coach.getId(), client.getId());
        aiMealPlanService.processJob(created.getId());

        Map<String, Object> payload = awaitJob(created.getId()).getOutputPayload();
        assertThat(payload).containsKeys("mealPlanId", "dayCount", "mealCount", "itemCount", "unmatchedCount");
        assertThat(((Number) payload.get("dayCount")).intValue()).isEqualTo(3);
        assertThat(((Number) payload.get("itemCount")).intValue()).isEqualTo(13);
        // The stub includes foods we do not stock, so some items need review.
        assertThat(((Number) payload.get("unmatchedCount")).intValue()).isPositive();
    }

    /** The full plan read must render a custom item under its own name. */
    @Test
    void getFullPlan_showsCustomItemsAsCustom() throws Exception {
        MealPlan plan = generate();

        mockMvc.perform(get("/api/v1/meal-plans/{id}", plan.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days.length()").value(3))
                .andExpect(jsonPath("$.data.days[0].meals[*].items[*].foodItemName")
                        .value(org.hamcrest.Matchers.hasItem("Grilled Tofu Steak")))
                .andExpect(jsonPath("$.data.days[0].meals[*].items[?(@.custom == true)]")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.empty())));
    }

    /** The counts reach the coach, not just the database. */
    @Test
    void getJob_exposesTheGenerationCounts() throws Exception {
        AiJob created = aiMealPlanService.createJob(coach.getId(), client.getId());
        aiMealPlanService.processJob(created.getId());
        awaitJob(created.getId());

        mockMvc.perform(get("/api/v1/ai/jobs/{id}", created.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.dayCount").value(3))
                .andExpect(jsonPath("$.data.itemCount").value(13))
                .andExpect(jsonPath("$.data.unmatchedCount").isNumber());
    }

    @Test
    void generateEndpoint_forAnotherCoachsClient_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/ai/meal-plans/generate")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("clientId", rivalClient.getId().toString()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void generateEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/ai/meal-plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("clientId", client.getId().toString()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getJob_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/ai/jobs/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }
}
