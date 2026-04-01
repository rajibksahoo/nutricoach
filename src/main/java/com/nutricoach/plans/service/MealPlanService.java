package com.nutricoach.plans.service;

import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.plans.dto.*;
import com.nutricoach.plans.entity.*;
import com.nutricoach.plans.mapper.MealPlanMapper;
import com.nutricoach.plans.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanDayRepository dayRepository;
    private final MealRepository mealRepository;
    private final MealItemRepository mealItemRepository;
    private final FoodItemRepository foodItemRepository;
    private final ClientRepository clientRepository;
    private final MealPlanMapper mealPlanMapper;

    // ── Meal Plans ────────────────────────────────────────────────────────────

    @Transactional
    public MealPlanSummaryResponse createPlan(UUID clientId, UUID coachId, CreateMealPlanRequest req) {
        requireClientOwned(clientId, coachId);

        MealPlan plan = MealPlan.builder()
                .coachId(coachId)
                .clientId(clientId)
                .name(req.name())
                .description(req.description())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .totalCaloriesTarget(req.totalCaloriesTarget())
                .build();

        return mealPlanMapper.toSummary(mealPlanRepository.save(plan));
    }

    @Transactional(readOnly = true)
    public List<MealPlanSummaryResponse> listPlans(UUID clientId, UUID coachId) {
        requireClientOwned(clientId, coachId);
        return mealPlanRepository.findByClientIdAndCoachIdAndDeletedAtIsNull(clientId, coachId)
                .stream().map(mealPlanMapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public MealPlanResponse getFullPlan(UUID planId, UUID coachId) {
        MealPlan plan = requirePlanOwned(planId, coachId);
        return buildFullResponse(plan);
    }

    /** Client portal variant — verifies planId belongs to both clientId AND coachId. */
    @Transactional(readOnly = true)
    public MealPlanResponse getFullPlanForClient(UUID planId, UUID clientId, UUID coachId) {
        MealPlan plan = mealPlanRepository
                .findByIdAndClientIdAndCoachIdAndDeletedAtIsNull(planId, clientId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Meal plan not found"));
        return buildFullResponse(plan);
    }

    private MealPlanResponse buildFullResponse(MealPlan plan) {
        List<MealPlanDay> days = dayRepository.findByMealPlanIdOrderByDayNumber(plan.getId());
        List<MealPlanDayResponse> dayResponses = days.stream().map(this::buildDayResponse).toList();
        return new MealPlanResponse(
                plan.getId(), plan.getClientId(), plan.getName(), plan.getDescription(),
                plan.getStartDate(), plan.getEndDate(), plan.getStatus(),
                plan.isAiGenerated(), plan.getTotalCaloriesTarget(),
                plan.getCreatedAt(), plan.getUpdatedAt(), dayResponses);
    }

    @Transactional
    public MealPlanSummaryResponse updatePlan(UUID planId, UUID coachId, UpdateMealPlanRequest req) {
        MealPlan plan = requirePlanOwned(planId, coachId);

        if (StringUtils.hasText(req.name()))    plan.setName(req.name());
        if (req.description() != null)          plan.setDescription(req.description());
        if (req.startDate() != null)            plan.setStartDate(req.startDate());
        if (req.endDate() != null)              plan.setEndDate(req.endDate());
        if (req.totalCaloriesTarget() != null)  plan.setTotalCaloriesTarget(req.totalCaloriesTarget());

        return mealPlanMapper.toSummary(mealPlanRepository.save(plan));
    }

    @Transactional
    public MealPlanSummaryResponse updateStatus(UUID planId, UUID coachId, MealPlan.Status status) {
        MealPlan plan = requirePlanOwned(planId, coachId);
        plan.setStatus(status);
        return mealPlanMapper.toSummary(mealPlanRepository.save(plan));
    }

    @Transactional
    public void deletePlan(UUID planId, UUID coachId) {
        MealPlan plan = requirePlanOwned(planId, coachId);
        plan.setDeletedAt(Instant.now());
        mealPlanRepository.save(plan);
    }

    // ── Days ──────────────────────────────────────────────────────────────────

    @Transactional
    public MealPlanDayResponse addDay(UUID planId, UUID coachId, int dayNumber) {
        requirePlanOwned(planId, coachId);

        if (dayNumber < 1 || dayNumber > 7) {
            throw NutriCoachException.badRequest("Day number must be between 1 and 7");
        }
        if (dayRepository.existsByMealPlanIdAndDayNumber(planId, dayNumber)) {
            throw NutriCoachException.conflict("Day " + dayNumber + " already exists in this plan");
        }

        MealPlanDay day = MealPlanDay.builder()
                .mealPlanId(planId)
                .dayNumber(dayNumber)
                .build();

        return buildDayResponse(dayRepository.save(day));
    }

    @Transactional
    public void removeDay(UUID planId, UUID coachId, UUID dayId) {
        requirePlanOwned(planId, coachId);
        MealPlanDay day = requireDay(dayId, planId);

        // Delete all items → meals under this day
        List<Meal> meals = mealRepository.findByMealPlanDayIdOrderBySequenceOrder(dayId);
        for (Meal meal : meals) {
            mealItemRepository.deleteAll(mealItemRepository.findByMealId(meal.getId()));
        }
        mealRepository.deleteAll(meals);
        dayRepository.delete(day);
    }

    // ── Meals ─────────────────────────────────────────────────────────────────

    @Transactional
    public MealResponse addMeal(UUID planId, UUID coachId, UUID dayId, AddMealRequest req) {
        requirePlanOwned(planId, coachId);
        requireDay(dayId, planId);

        Meal meal = Meal.builder()
                .mealPlanDayId(dayId)
                .mealType(req.mealType())
                .name(req.name())
                .timeOfDay(req.timeOfDay())
                .sequenceOrder(req.sequenceOrder())
                .build();

        return buildMealResponseFromEntities(mealRepository.save(meal), List.of());
    }

    @Transactional
    public MealResponse updateMeal(UUID planId, UUID coachId, UUID dayId, UUID mealId, UpdateMealRequest req) {
        requirePlanOwned(planId, coachId);
        requireDay(dayId, planId);
        Meal meal = requireMeal(mealId, dayId);

        if (req.mealType() != null)             meal.setMealType(req.mealType());
        if (StringUtils.hasText(req.name()))    meal.setName(req.name());
        if (req.timeOfDay() != null)            meal.setTimeOfDay(req.timeOfDay());
        if (req.sequenceOrder() != null)        meal.setSequenceOrder(req.sequenceOrder());

        List<MealItem> items = mealItemRepository.findByMealId(mealId);
        return buildMealResponseFromEntities(mealRepository.save(meal), items);
    }

    @Transactional
    public void removeMeal(UUID planId, UUID coachId, UUID dayId, UUID mealId) {
        requirePlanOwned(planId, coachId);
        requireDay(dayId, planId);
        Meal meal = requireMeal(mealId, dayId);

        mealItemRepository.deleteAll(mealItemRepository.findByMealId(mealId));
        mealRepository.delete(meal);
        recalculateDayTotals(dayId);
    }

    // ── Meal Items ────────────────────────────────────────────────────────────

    @Transactional
    public MealItemResponse addItem(UUID planId, UUID coachId, UUID dayId, UUID mealId, AddMealItemRequest req) {
        requirePlanOwned(planId, coachId);
        requireDay(dayId, planId);
        requireMeal(mealId, dayId);

        FoodItem food = foodItemRepository.findById(req.foodItemId())
                .orElseThrow(() -> NutriCoachException.notFound("Food item not found"));

        BigDecimal factor = req.quantityGrams().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        String unit = StringUtils.hasText(req.quantityUnit()) ? req.quantityUnit() : "g";

        MealItem item = MealItem.builder()
                .mealId(mealId)
                .foodItemId(food.getId())
                .quantityGrams(req.quantityGrams())
                .quantityUnit(unit)
                .calories(food.getCaloriesPer100g().multiply(factor).setScale(0, RoundingMode.HALF_UP).intValue())
                .proteinG(food.getProteinPer100g().multiply(factor).setScale(2, RoundingMode.HALF_UP))
                .carbsG(food.getCarbsPer100g().multiply(factor).setScale(2, RoundingMode.HALF_UP))
                .fatG(food.getFatPer100g().multiply(factor).setScale(2, RoundingMode.HALF_UP))
                .build();

        MealItem saved = mealItemRepository.save(item);
        recalculateDayTotals(dayId);

        return toItemResponse(saved, food.getName());
    }

    @Transactional
    public void removeItem(UUID planId, UUID coachId, UUID dayId, UUID mealId, UUID itemId) {
        requirePlanOwned(planId, coachId);
        requireDay(dayId, planId);
        requireMeal(mealId, dayId);

        MealItem item = mealItemRepository.findByIdAndMealId(itemId, mealId)
                .orElseThrow(() -> NutriCoachException.notFound("Meal item not found"));

        mealItemRepository.delete(item);
        recalculateDayTotals(dayId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void recalculateDayTotals(UUID dayId) {
        List<MealItem> allItems = mealItemRepository.findByDayId(dayId);

        int totalCals = allItems.stream().mapToInt(i -> i.getCalories() != null ? i.getCalories() : 0).sum();
        BigDecimal totalProtein = allItems.stream().map(i -> nvl(i.getProteinG())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCarbs   = allItems.stream().map(i -> nvl(i.getCarbsG())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFat     = allItems.stream().map(i -> nvl(i.getFatG())).reduce(BigDecimal.ZERO, BigDecimal::add);

        dayRepository.findById(dayId).ifPresent(day -> {
            day.setTotalCalories(totalCals);
            day.setTotalProteinG(totalProtein.setScale(2, RoundingMode.HALF_UP));
            day.setTotalCarbsG(totalCarbs.setScale(2, RoundingMode.HALF_UP));
            day.setTotalFatG(totalFat.setScale(2, RoundingMode.HALF_UP));
            dayRepository.save(day);
        });
    }

    private MealPlanDayResponse buildDayResponse(MealPlanDay day) {
        List<Meal> meals = mealRepository.findByMealPlanDayIdOrderBySequenceOrder(day.getId());

        // Batch-load all items + food names for this day to avoid N+1
        List<MealItem> allItems = mealItemRepository.findByDayId(day.getId());
        Map<UUID, List<MealItem>> itemsByMealId = allItems.stream()
                .collect(Collectors.groupingBy(MealItem::getMealId));
        List<UUID> foodIds = allItems.stream().map(MealItem::getFoodItemId).distinct().toList();
        Map<UUID, String> foodNames = foodItemRepository.findAllById(foodIds).stream()
                .collect(Collectors.toMap(FoodItem::getId, FoodItem::getName));

        List<MealResponse> mealResponses = meals.stream().map(meal -> {
            List<MealItem> mealItems = itemsByMealId.getOrDefault(meal.getId(), List.of());
            List<MealItemResponse> itemResponses = mealItems.stream()
                    .map(item -> toItemResponse(item, foodNames.getOrDefault(item.getFoodItemId(), "Unknown")))
                    .toList();
            return toMealResponse(meal, itemResponses);
        }).toList();

        return new MealPlanDayResponse(day.getId(), day.getDayNumber(),
                day.getTotalCalories(), day.getTotalProteinG(), day.getTotalCarbsG(), day.getTotalFatG(),
                mealResponses);
    }

    private MealResponse buildMealResponseFromEntities(Meal meal, List<MealItem> items) {
        List<UUID> foodIds = items.stream().map(MealItem::getFoodItemId).distinct().toList();
        Map<UUID, String> foodNames = foodIds.isEmpty() ? Map.of() :
                foodItemRepository.findAllById(foodIds).stream()
                        .collect(Collectors.toMap(FoodItem::getId, FoodItem::getName));
        List<MealItemResponse> itemResponses = items.stream()
                .map(i -> toItemResponse(i, foodNames.getOrDefault(i.getFoodItemId(), "Unknown")))
                .toList();
        return toMealResponse(meal, itemResponses);
    }

    private MealResponse toMealResponse(Meal meal, List<MealItemResponse> itemResponses) {
        return new MealResponse(meal.getId(), meal.getMealType(), meal.getName(),
                meal.getTimeOfDay(), meal.getSequenceOrder(), itemResponses);
    }

    private MealItemResponse toItemResponse(MealItem item, String foodItemName) {
        return new MealItemResponse(item.getId(), item.getFoodItemId(), foodItemName,
                item.getQuantityGrams(), item.getQuantityUnit(),
                item.getCalories(), item.getProteinG(), item.getCarbsG(), item.getFatG());
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    private void requireClientOwned(UUID clientId, UUID coachId) {
        if (!clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(clientId, coachId).isPresent()) {
            throw NutriCoachException.notFound("Client not found");
        }
    }

    private MealPlan requirePlanOwned(UUID planId, UUID coachId) {
        return mealPlanRepository.findByIdAndCoachIdAndDeletedAtIsNull(planId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Meal plan not found"));
    }

    private MealPlanDay requireDay(UUID dayId, UUID planId) {
        return dayRepository.findByIdAndMealPlanId(dayId, planId)
                .orElseThrow(() -> NutriCoachException.notFound("Day not found in this plan"));
    }

    private Meal requireMeal(UUID mealId, UUID dayId) {
        return mealRepository.findByIdAndMealPlanDayId(mealId, dayId)
                .orElseThrow(() -> NutriCoachException.notFound("Meal not found in this day"));
    }
}
