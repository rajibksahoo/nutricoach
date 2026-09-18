package com.nutricoach.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.ai.entity.AiJob;
import com.nutricoach.ai.repository.AiJobRepository;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.common.config.OpenAiProperties;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.plans.entity.FoodItem;
import com.nutricoach.plans.entity.Meal;
import com.nutricoach.plans.entity.MealItem;
import com.nutricoach.plans.entity.MealPlan;
import com.nutricoach.plans.entity.MealPlanDay;
import com.nutricoach.plans.repository.FoodItemRepository;
import com.nutricoach.plans.repository.MealItemRepository;
import com.nutricoach.plans.repository.MealPlanDayRepository;
import com.nutricoach.plans.repository.MealPlanRepository;
import com.nutricoach.plans.repository.MealRepository;
import com.nutricoach.plans.service.MealPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(OpenAiProperties.class)
public class AiMealPlanService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    /**
     * Returned instead of calling OpenAI when the API key starts with "local-".
     *
     * <p>Deliberately mixes foods the curated list holds under a qualified name
     * ("Poha" resolves to "Poha (cooked)", "Idli" to "Idli (steamed)") with ones
     * it does not hold at all (Grilled Tofu Steak, Quinoa Upma, Millet Khichdi),
     * so the parser exercises the alias index, the exact index and the unmatched
     * branch without spending tokens. Unmatched items carry their own macros, as
     * the prompt requires of the real model.
     */
    private static final String LOCAL_STUB_RESPONSE = """
            {
              "name": "7-Day Weight Loss Plan",
              "days": [
                {
                  "dayNumber": 1,
                  "meals": [
                    {
                      "mealName": "Breakfast",
                      "items": [{"foodItemName": "Poha", "quantityGrams": 100, "calories": 130, "proteinG": 2.5, "carbsG": 27.0, "fatG": 1.5}]
                    },
                    {
                      "mealName": "Lunch",
                      "items": [
                        {"foodItemName": "Idli", "quantityGrams": 120, "calories": 156, "proteinG": 4.6, "carbsG": 33.0, "fatG": 0.5},
                        {"foodItemName": "Quinoa Upma", "quantityGrams": 150, "calories": 210, "proteinG": 7.2, "carbsG": 34.5, "fatG": 4.8}
                      ]
                    },
                    {
                      "mealName": "Evening Snack",
                      "items": [{"foodItemName": "Banana", "quantityGrams": 100, "calories": 89, "proteinG": 1.1, "carbsG": 22.8, "fatG": 0.3}]
                    },
                    {
                      "mealName": "Dinner",
                      "items": [{"foodItemName": "Grilled Tofu Steak", "quantityGrams": 150, "calories": 220, "proteinG": 24.0, "carbsG": 4.5, "fatG": 12.0}]
                    }
                  ]
                },
                {
                  "dayNumber": 2,
                  "meals": [
                    {
                      "mealName": "Breakfast",
                      "items": [{"foodItemName": "Idli", "quantityGrams": 150, "calories": 195, "proteinG": 5.8, "carbsG": 41.3, "fatG": 0.6}]
                    },
                    {
                      "mealName": "Lunch",
                      "items": [{"foodItemName": "Banana", "quantityGrams": 120, "calories": 107, "proteinG": 1.3, "carbsG": 27.4, "fatG": 0.4}]
                    },
                    {
                      "mealName": "Evening Snack",
                      "items": [{"foodItemName": "Apple", "quantityGrams": 150, "calories": 78, "proteinG": 0.4, "carbsG": 20.7, "fatG": 0.3}]
                    },
                    {
                      "mealName": "Dinner",
                      "items": [{"foodItemName": "Millet Khichdi", "quantityGrams": 200, "calories": 260, "proteinG": 8.4, "carbsG": 48.0, "fatG": 4.2}]
                    }
                  ]
                },
                {
                  "dayNumber": 3,
                  "meals": [
                    {
                      "mealName": "Breakfast",
                      "items": [{"foodItemName": "Poha", "quantityGrams": 120, "calories": 156, "proteinG": 3.0, "carbsG": 32.4, "fatG": 1.8}]
                    },
                    {
                      "mealName": "Lunch",
                      "items": [{"foodItemName": "Idli", "quantityGrams": 120, "calories": 156, "proteinG": 4.6, "carbsG": 33.0, "fatG": 0.5}]
                    },
                    {
                      "mealName": "Evening Snack",
                      "items": [{"foodItemName": "Apple", "quantityGrams": 100, "calories": 52, "proteinG": 0.3, "carbsG": 13.8, "fatG": 0.2}]
                    },
                    {
                      "mealName": "Dinner",
                      "items": [{"foodItemName": "Grilled Tofu Steak", "quantityGrams": 120, "calories": 176, "proteinG": 19.2, "carbsG": 3.6, "fatG": 9.6}]
                    }
                  ]
                }
              ]
            }
            """;

    private final AiJobRepository aiJobRepository;
    private final ClientRepository clientRepository;
    private final MealPlanRepository mealPlanRepository;
    private final MealPlanDayRepository dayRepository;
    private final MealRepository mealRepository;
    private final MealItemRepository mealItemRepository;
    private final FoodItemRepository foodItemRepository;
    private final MealPlanService mealPlanService;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new AI job with PENDING status after verifying the client belongs to the coach.
     */
    @Transactional
    public AiJob createJob(UUID coachId, UUID clientId) {
        clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(clientId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Client not found"));

        AiJob job = AiJob.builder()
                .coachId(coachId)
                .clientId(clientId)
                .jobType(AiJob.JobType.MEAL_PLAN_GENERATION)
                .status(AiJob.Status.PENDING)
                .build();

        return aiJobRepository.save(job);
    }

    /**
     * Processes the AI job asynchronously.
     * Calls OpenAI (or uses a stub for local keys), parses the response,
     * saves a MealPlan, and updates the job status.
     */
    @Async
    @Transactional
    public void processJob(UUID jobId) {
        AiJob job = aiJobRepository.findById(jobId)
                .orElseThrow(() -> NutriCoachException.notFound("AI job not found: " + jobId));

        job.setStatus(AiJob.Status.PROCESSING);
        job.setStartedAt(Instant.now());
        aiJobRepository.save(job);

        try {
            Client client = clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(job.getClientId(), job.getCoachId())
                    .orElseThrow(() -> NutriCoachException.notFound("Client not found for job"));

            String aiContent = callOpenAi(client);

            JsonNode root = objectMapper.readTree(aiContent);
            String planName = root.path("name").asText("7-Day AI Meal Plan");

            MealPlan mealPlan = MealPlan.builder()
                    .coachId(job.getCoachId())
                    .clientId(job.getClientId())
                    .name(planName)
                    .aiGenerated(true)
                    .status(MealPlan.Status.DRAFT)
                    .build();

            MealPlan saved = mealPlanRepository.save(mealPlan);
            PersistResult result = persistDays(root, saved.getId());

            job.setStatus(AiJob.Status.COMPLETED);
            job.setCompletedAt(Instant.now());
            job.setOutputPayload(Map.of(
                    "mealPlanId", saved.getId().toString(),
                    "dayCount", result.days(),
                    "mealCount", result.meals(),
                    "itemCount", result.items(),
                    "unmatchedCount", result.unmatched()));
            aiJobRepository.save(job);

            log.info("AiMealPlanService: job={} completed, mealPlanId={}", jobId, saved.getId());

        } catch (Exception e) {
            log.error("AiMealPlanService: job={} failed: {}", jobId, e.getMessage(), e);
            job.setStatus(AiJob.Status.FAILED);
            job.setCompletedAt(Instant.now());
            job.setErrorMessage(e.getMessage());
            aiJobRepository.save(job);
        }
    }

    /** What a generated plan actually produced, for the job payload. */
    private record PersistResult(int days, int meals, int items, int unmatched) {}

    /**
     * Writes the generated days, meals and items under a plan.
     *
     * <p>Before this existed the response was read for its {@code name} and then
     * discarded, so "generate with AI" reliably produced an empty DRAFT plan
     * while the UI claimed a 7-day plan had been created.
     *
     * <p>Food names are matched case-insensitively against {@code food_items}. On
     * a match the macros are recomputed from our own per-100g data rather than
     * trusting the model arithmetic; on a miss the item keeps the name the model
     * gave it plus the model macros, and is counted in {@code unmatchedCount} so
     * the coach knows what to review.
     */
    private PersistResult persistDays(JsonNode root, UUID planId) {
        Map<String, FoodItem> foodsByName = buildFoodIndex();

        int dayCount = 0, mealCount = 0, itemCount = 0, unmatched = 0;

        for (JsonNode dayNode : root.path("days")) {
            int dayNumber = dayNode.path("dayNumber").asInt(dayCount + 1);
            MealPlanDay day = dayRepository.save(MealPlanDay.builder()
                    .mealPlanId(planId)
                    .dayNumber(dayNumber)
                    .build());
            dayCount++;

            int sequence = 0;
            for (JsonNode mealNode : dayNode.path("meals")) {
                String mealName = mealNode.path("mealName").asText("Meal");
                Meal meal = mealRepository.save(Meal.builder()
                        .mealPlanDayId(day.getId())
                        .mealType(mealTypeOf(mealName))
                        .name(mealName)
                        .sequenceOrder(sequence++)
                        .build());
                mealCount++;

                for (JsonNode itemNode : mealNode.path("items")) {
                    String foodName = itemNode.path("foodItemName").asText("").trim();
                    if (foodName.isEmpty()) continue;

                    BigDecimal grams = decimal(itemNode, "quantityGrams", BigDecimal.valueOf(100));
                    FoodItem food = foodsByName.get(foodName.toLowerCase(Locale.ROOT));

                    MealItem.MealItemBuilder item = MealItem.builder()
                            .mealId(meal.getId())
                            .quantityGrams(grams)
                            .quantityUnit("g");

                    if (food != null) {
                        BigDecimal factor = grams.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                        item.foodItemId(food.getId())
                                .calories(food.getCaloriesPer100g().multiply(factor).setScale(0, RoundingMode.HALF_UP).intValue())
                                .proteinG(food.getProteinPer100g().multiply(factor).setScale(2, RoundingMode.HALF_UP))
                                .carbsG(food.getCarbsPer100g().multiply(factor).setScale(2, RoundingMode.HALF_UP))
                                .fatG(food.getFatPer100g().multiply(factor).setScale(2, RoundingMode.HALF_UP));
                    } else {
                        unmatched++;
                        item.customName(foodName.length() > 200 ? foodName.substring(0, 200) : foodName)
                                .calories(itemNode.path("calories").isNumber() ? itemNode.path("calories").asInt() : null)
                                .proteinG(decimal(itemNode, "proteinG", null))
                                .carbsG(decimal(itemNode, "carbsG", null))
                                .fatG(decimal(itemNode, "fatG", null));
                    }

                    mealItemRepository.save(item.build());
                    itemCount++;
                }
            }

            mealPlanService.recalculateDayTotals(day.getId());
        }

        return new PersistResult(dayCount, mealCount, itemCount, unmatched);
    }

    /**
     * Lookup keys for the curated food list, aliases included.
     *
     * <p>Seeded names carry qualifiers a model never writes: "Idli (steamed)",
     * "Poha (cooked)", "Chapati / Roti (cooked)". Matching on the full string
     * alone would miss nearly every generated item and push it into the
     * unmatched pile, wasting the macros we already hold. So each food is
     * indexed under its full name, its name minus any parenthetical, and each
     * "/"-separated alternative.
     *
     * <p>Aliases collide: "Poha" matches both "Poha (cooked)" at 130 kcal and
     * "Poha (dry, flattened rice)" at 356 kcal. Iterating in name order and
     * keeping the first writer makes the winner deterministic instead of
     * dependent on row order, and happens to favour the cooked form, which is
     * what a meal plan means. The coach can still correct the item.
     */
    private Map<String, FoodItem> buildFoodIndex() {
        Map<String, FoodItem> index = new HashMap<>();
        List<FoodItem> foods = new ArrayList<>(foodItemRepository.findAll());
        foods.sort(Comparator.comparing(FoodItem::getName, String.CASE_INSENSITIVE_ORDER));
        for (FoodItem food : foods) {
            String full = food.getName().toLowerCase(Locale.ROOT).trim();
            index.putIfAbsent(full, food);

            String base = full;
            int paren = base.indexOf('(');
            if (paren > 0) base = base.substring(0, paren).trim();
            if (!base.isEmpty()) index.putIfAbsent(base, food);

            for (String alias : base.split("/")) {
                String a = alias.trim();
                if (!a.isEmpty()) index.putIfAbsent(a, food);
            }
        }
        return index;
    }

    /** Maps the free-text meal name onto the stored enum. */
    private static Meal.MealType mealTypeOf(String mealName) {
        String n = mealName.toLowerCase(Locale.ROOT);
        if (n.contains("breakfast")) return Meal.MealType.BREAKFAST;
        if (n.contains("lunch")) return Meal.MealType.LUNCH;
        if (n.contains("dinner")) return Meal.MealType.DINNER;
        if (n.contains("pre")) return Meal.MealType.PRE_WORKOUT;
        if (n.contains("post")) return Meal.MealType.POST_WORKOUT;
        return Meal.MealType.SNACK;
    }

    private static BigDecimal decimal(JsonNode node, String field, BigDecimal fallback) {
        JsonNode v = node.path(field);
        return v.isNumber() ? v.decimalValue() : fallback;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String callOpenAi(Client client) throws Exception {
        String apiKey = openAiProperties.getApiKey();

        if (apiKey != null && apiKey.startsWith("local-")) {
            log.debug("AiMealPlanService: using local stub response (apiKey starts with 'local-')");
            return LOCAL_STUB_RESPONSE;
        }

        String systemPrompt = "You are a professional nutritionist. Generate a 7-day Indian meal plan in valid JSON format only. No markdown, no explanation, just the JSON object.";

        String goal = client.getGoal() != null ? client.getGoal().name() : "MAINTENANCE";
        String dietaryPref = client.getDietaryPref() != null ? client.getDietaryPref().name() : "VEG";
        String activityLevel = client.getActivityLevel() != null ? client.getActivityLevel().name() : "MODERATE";
        String healthConditions = formatList(client.getHealthConditions());
        String allergies = formatList(client.getAllergies());

        String userPrompt = String.format("""
                Create a 7-day meal plan for a client with these details:
                - Name: %s
                - Goal: %s
                - Dietary preference: %s
                - Activity level: %s
                - Health conditions: %s
                - Allergies: %s

                Return ONLY this JSON structure:
                {"name": "7-Day %s Plan", "days": [{"dayNumber": 1, "meals": [{"mealName": "Breakfast", "items": [{"foodItemName": "Oats", "quantityGrams": 80, "calories": 303, "proteinG": 10.6, "carbsG": 55.2, "fatG": 5.3}]}]}]}

                Include calories, proteinG, carbsG and fatG for the stated quantity of
                every item. We recompute these ourselves for foods we stock, but a food
                we do not stock has no other source of macros.

                Use common Indian foods. Include Breakfast, Lunch, Evening Snack, and Dinner for each day.""",
                client.getName(), goal, dietaryPref, activityLevel, healthConditions, allergies, goal);

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", 4000
        );

        RestClient restClient = RestClient.create();

        String responseBody = restClient.post()
                .uri(OPENAI_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);

        JsonNode responseNode = objectMapper.readTree(responseBody);
        return responseNode.path("choices").get(0).path("message").path("content").asText();
    }

    private String formatList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "None";
        }
        return String.join(", ", items);
    }
}
