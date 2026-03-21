package com.nutricoach.plans;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.plans.entity.FoodItem;
import com.nutricoach.plans.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MealPlanIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired MealPlanRepository mealPlanRepository;
    @Autowired MealPlanDayRepository dayRepository;
    @Autowired MealRepository mealRepository;
    @Autowired MealItemRepository mealItemRepository;
    @Autowired FoodItemRepository foodItemRepository;
    @Autowired JwtService jwtService;

    private String jwt;
    private Coach coach;
    private Client client;
    private UUID foodItemId;

    @BeforeEach
    void setup() {
        // Clean up in FK order: items → meals → days → plans → clients → coach
        coachRepository.findByPhone("9800000001").ifPresent(existing -> {
            clientRepository.findAllByCoachId(existing.getId()).forEach(c -> {
                mealPlanRepository.findByClientIdAndCoachIdAndDeletedAtIsNull(c.getId(), existing.getId())
                        .forEach(plan -> {
                            dayRepository.findByMealPlanIdOrderByDayNumber(plan.getId()).forEach(day -> {
                                mealRepository.findByMealPlanDayIdOrderBySequenceOrder(day.getId()).forEach(meal -> {
                                    mealItemRepository.deleteAll(mealItemRepository.findByMealId(meal.getId()));
                                });
                                mealRepository.deleteAll(mealRepository.findByMealPlanDayIdOrderBySequenceOrder(day.getId()));
                            });
                            dayRepository.deleteAll(dayRepository.findByMealPlanIdOrderByDayNumber(plan.getId()));
                        });
                // Also delete soft-deleted plans
                mealPlanRepository.findAll().stream()
                        .filter(p -> p.getCoachId().equals(existing.getId()) && p.getClientId().equals(c.getId()))
                        .forEach(plan -> {
                            dayRepository.findByMealPlanIdOrderByDayNumber(plan.getId()).forEach(day -> {
                                mealRepository.findByMealPlanDayIdOrderBySequenceOrder(day.getId()).forEach(meal ->
                                        mealItemRepository.deleteAll(mealItemRepository.findByMealId(meal.getId())));
                                mealRepository.deleteAll(mealRepository.findByMealPlanDayIdOrderBySequenceOrder(day.getId()));
                            });
                            dayRepository.deleteAll(dayRepository.findByMealPlanIdOrderByDayNumber(plan.getId()));
                            mealPlanRepository.delete(plan);
                        });
            });
            clientRepository.deleteAll(clientRepository.findAllByCoachId(existing.getId()));
            coachRepository.delete(existing);
        });

        coach = coachRepository.save(Coach.builder()
                .phone("9800000001")
                .name("Plan Test Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());

        client = clientRepository.save(Client.builder()
                .coachId(coach.getId())
                .phone("9800000002")
                .name("Plan Test Client")
                .status(Client.Status.ACTIVE)
                .build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");

        // Use a seeded food item (rice) for item-addition tests
        foodItemId = foodItemRepository.search("Basmati Rice", null, null)
                .stream().findFirst()
                .or(() -> foodItemRepository.search("rice", null, null).stream().findFirst())
                .orElseThrow(() -> new IllegalStateException("Seed food item not found"))
                .getId();
    }

    // ── Plan CRUD ─────────────────────────────────────────────────────────────

    @Test
    void createPlan_validRequest_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/clients/{id}/meal-plans", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "High Protein Plan",
                                "totalCaloriesTarget", 2000
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("High Protein Plan"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.clientId").value(client.getId().toString()));
    }

    @Test
    void createPlan_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/clients/{id}/meal-plans", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "Missing name"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createPlan_wrongClient_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/clients/{id}/meal-plans", UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Orphan Plan"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void listPlans_returnsClientPlans() throws Exception {
        // Create two plans
        mockMvc.perform(post("/api/v1/clients/{id}/meal-plans", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Plan A"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/clients/{id}/meal-plans", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Plan B"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/clients/{id}/meal-plans", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void updatePlan_patchesName() throws Exception {
        String planId = createPlanAndGetId("Original Name");

        mockMvc.perform(put("/api/v1/meal-plans/{id}", planId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Updated Name"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Name"));
    }

    @Test
    void updateStatus_setsActive() throws Exception {
        String planId = createPlanAndGetId("Status Test Plan");

        mockMvc.perform(patch("/api/v1/meal-plans/{id}/status?status=ACTIVE", planId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void deletePlan_softDeletes_notInList() throws Exception {
        String planId = createPlanAndGetId("To Be Deleted");

        mockMvc.perform(delete("/api/v1/meal-plans/{id}", planId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        // Should no longer appear in list
        mockMvc.perform(get("/api/v1/clients/{id}/meal-plans", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + planId + "')]").doesNotExist());
    }

    @Test
    void getPlan_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{id}/meal-plans", client.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ── Days ──────────────────────────────────────────────────────────────────

    @Test
    void addDay_validDayNumber_returns201() throws Exception {
        String planId = createPlanAndGetId("Day Test Plan");

        mockMvc.perform(post("/api/v1/meal-plans/{id}/days?dayNumber=1", planId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dayNumber").value(1));
    }

    @Test
    void addDay_duplicate_returns409() throws Exception {
        String planId = createPlanAndGetId("Dup Day Plan");

        mockMvc.perform(post("/api/v1/meal-plans/{id}/days?dayNumber=3", planId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/meal-plans/{id}/days?dayNumber=3", planId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isConflict());
    }

    @Test
    void addDay_outOfRange_returns400() throws Exception {
        String planId = createPlanAndGetId("Bad Day Plan");

        mockMvc.perform(post("/api/v1/meal-plans/{id}/days?dayNumber=8", planId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeDay_deletesFromPlan() throws Exception {
        String planId = createPlanAndGetId("Remove Day Plan");

        String dayId = mockMvc.perform(post("/api/v1/meal-plans/{id}/days?dayNumber=2", planId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String extractedDayId = objectMapper.readTree(dayId).path("data").path("id").asText();

        mockMvc.perform(delete("/api/v1/meal-plans/{planId}/days/{dayId}", planId, extractedDayId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        // Full plan should show no days
        mockMvc.perform(get("/api/v1/meal-plans/{id}", planId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days.length()").value(0));
    }

    // ── Meals ─────────────────────────────────────────────────────────────────

    @Test
    void addMeal_validRequest_returns201() throws Exception {
        String planId = createPlanAndGetId("Meal Test Plan");
        String dayId = addDayAndGetId(planId, 1);

        mockMvc.perform(post("/api/v1/meal-plans/{planId}/days/{dayId}/meals", planId, dayId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "mealType", "BREAKFAST",
                                "name", "Morning Oats",
                                "sequenceOrder", 1
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.mealType").value("BREAKFAST"))
                .andExpect(jsonPath("$.data.name").value("Morning Oats"))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void addMeal_missingName_returns400() throws Exception {
        String planId = createPlanAndGetId("Meal Validation Plan");
        String dayId = addDayAndGetId(planId, 1);

        mockMvc.perform(post("/api/v1/meal-plans/{planId}/days/{dayId}/meals", planId, dayId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "mealType", "LUNCH",
                                "sequenceOrder", 1
                        ))))
                .andExpect(status().isBadRequest());
    }

    // ── Meal Items ────────────────────────────────────────────────────────────

    @Test
    void addItem_calculatesNutrition() throws Exception {
        String planId = createPlanAndGetId("Nutrition Calc Plan");
        String dayId = addDayAndGetId(planId, 1);
        String mealId = addMealAndGetId(planId, dayId, "BREAKFAST", "Test Meal");

        mockMvc.perform(post("/api/v1/meal-plans/{planId}/days/{dayId}/meals/{mealId}/items",
                                planId, dayId, mealId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "foodItemId", foodItemId.toString(),
                                "quantityGrams", new BigDecimal("100")
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.quantityGrams").value(100))
                .andExpect(jsonPath("$.data.calories").isNumber())
                .andExpect(jsonPath("$.data.proteinG").isNumber())
                .andExpect(jsonPath("$.data.carbsG").isNumber())
                .andExpect(jsonPath("$.data.fatG").isNumber());
    }

    @Test
    void addItem_recalculatesDayTotals() throws Exception {
        String planId = createPlanAndGetId("Day Totals Plan");
        String dayId = addDayAndGetId(planId, 1);
        String mealId = addMealAndGetId(planId, dayId, "LUNCH", "Lunch Meal");

        // Add item
        String itemResponse = mockMvc.perform(
                        post("/api/v1/meal-plans/{planId}/days/{dayId}/meals/{mealId}/items",
                                planId, dayId, mealId)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "foodItemId", foodItemId.toString(),
                                        "quantityGrams", new BigDecimal("100")
                                ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String itemId = objectMapper.readTree(itemResponse).path("data").path("id").asText();

        // Full plan should show non-zero day totals
        mockMvc.perform(get("/api/v1/meal-plans/{id}", planId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[0].totalCalories").value(greaterThanOrEqualTo(1)));

        // Remove item — totals should reset to 0
        mockMvc.perform(delete("/api/v1/meal-plans/{planId}/days/{dayId}/meals/{mealId}/items/{itemId}",
                                planId, dayId, mealId, itemId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/meal-plans/{id}", planId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[0].totalCalories").value(0));
    }

    @Test
    void addItem_unknownFoodItem_returns404() throws Exception {
        String planId = createPlanAndGetId("Bad Item Plan");
        String dayId = addDayAndGetId(planId, 1);
        String mealId = addMealAndGetId(planId, dayId, "DINNER", "Dinner Meal");

        mockMvc.perform(post("/api/v1/meal-plans/{planId}/days/{dayId}/meals/{mealId}/items",
                                planId, dayId, mealId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "foodItemId", UUID.randomUUID().toString(),
                                "quantityGrams", new BigDecimal("100")
                        ))))
                .andExpect(status().isNotFound());
    }

    // ── Full plan structure ───────────────────────────────────────────────────

    @Test
    void getFullPlan_returnsDaysAndMeals() throws Exception {
        String planId = createPlanAndGetId("Full Structure Plan");
        String dayId = addDayAndGetId(planId, 1);
        addMealAndGetId(planId, dayId, "BREAKFAST", "Morning Meal");

        mockMvc.perform(get("/api/v1/meal-plans/{id}", planId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(planId))
                .andExpect(jsonPath("$.data.days").isArray())
                .andExpect(jsonPath("$.data.days.length()").value(1))
                .andExpect(jsonPath("$.data.days[0].dayNumber").value(1))
                .andExpect(jsonPath("$.data.days[0].meals").isArray())
                .andExpect(jsonPath("$.data.days[0].meals.length()").value(1));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String createPlanAndGetId(String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/clients/{id}/meal-plans", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asText();
    }

    private String addDayAndGetId(String planId, int dayNumber) throws Exception {
        String response = mockMvc.perform(
                        post("/api/v1/meal-plans/{id}/days?dayNumber=" + dayNumber, planId)
                                .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asText();
    }

    private String addMealAndGetId(String planId, String dayId, String mealType, String name) throws Exception {
        String response = mockMvc.perform(
                        post("/api/v1/meal-plans/{planId}/days/{dayId}/meals", planId, dayId)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "mealType", mealType,
                                        "name", name,
                                        "sequenceOrder", 1
                                ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asText();
    }
}
