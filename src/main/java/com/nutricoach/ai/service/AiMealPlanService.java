package com.nutricoach.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.ai.entity.AiJob;
import com.nutricoach.ai.repository.AiJobRepository;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.common.config.OpenAiProperties;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.plans.entity.MealPlan;
import com.nutricoach.plans.repository.MealPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(OpenAiProperties.class)
public class AiMealPlanService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    // Hardcoded fallback response used when apiKey starts with "local-"
    private static final String LOCAL_STUB_RESPONSE = """
            {
              "name": "7-Day Weight Loss Plan",
              "days": [
                {
                  "dayNumber": 1,
                  "meals": [
                    {
                      "mealName": "Breakfast",
                      "items": [{"foodItemName": "Oats", "quantityGrams": 80}]
                    },
                    {
                      "mealName": "Lunch",
                      "items": [{"foodItemName": "Dal", "quantityGrams": 150}, {"foodItemName": "Brown Rice", "quantityGrams": 100}]
                    },
                    {
                      "mealName": "Evening Snack",
                      "items": [{"foodItemName": "Banana", "quantityGrams": 100}]
                    },
                    {
                      "mealName": "Dinner",
                      "items": [{"foodItemName": "Chapati", "quantityGrams": 120}, {"foodItemName": "Sabzi", "quantityGrams": 150}]
                    }
                  ]
                },
                {
                  "dayNumber": 2,
                  "meals": [
                    {
                      "mealName": "Breakfast",
                      "items": [{"foodItemName": "Poha", "quantityGrams": 100}]
                    },
                    {
                      "mealName": "Lunch",
                      "items": [{"foodItemName": "Rajma", "quantityGrams": 150}, {"foodItemName": "Rice", "quantityGrams": 100}]
                    },
                    {
                      "mealName": "Evening Snack",
                      "items": [{"foodItemName": "Apple", "quantityGrams": 150}]
                    },
                    {
                      "mealName": "Dinner",
                      "items": [{"foodItemName": "Chapati", "quantityGrams": 120}, {"foodItemName": "Dal Fry", "quantityGrams": 150}]
                    }
                  ]
                },
                {
                  "dayNumber": 3,
                  "meals": [
                    {
                      "mealName": "Breakfast",
                      "items": [{"foodItemName": "Idli", "quantityGrams": 120}]
                    },
                    {
                      "mealName": "Lunch",
                      "items": [{"foodItemName": "Sambar", "quantityGrams": 150}, {"foodItemName": "Brown Rice", "quantityGrams": 100}]
                    },
                    {
                      "mealName": "Evening Snack",
                      "items": [{"foodItemName": "Roasted Chana", "quantityGrams": 30}]
                    },
                    {
                      "mealName": "Dinner",
                      "items": [{"foodItemName": "Chapati", "quantityGrams": 120}, {"foodItemName": "Paneer Curry", "quantityGrams": 150}]
                    }
                  ]
                }
              ]
            }
            """;

    private final AiJobRepository aiJobRepository;
    private final ClientRepository clientRepository;
    private final MealPlanRepository mealPlanRepository;
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

            job.setStatus(AiJob.Status.COMPLETED);
            job.setCompletedAt(Instant.now());
            job.setOutputPayload(Map.of("mealPlanId", saved.getId().toString()));
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
                {"name": "7-Day %s Plan", "days": [{"dayNumber": 1, "meals": [{"mealName": "Breakfast", "items": [{"foodItemName": "Oats", "quantityGrams": 80}]}]}]}

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
