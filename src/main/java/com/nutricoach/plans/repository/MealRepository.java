package com.nutricoach.plans.repository;

import com.nutricoach.plans.entity.Meal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealRepository extends JpaRepository<Meal, UUID> {

    List<Meal> findByMealPlanDayIdOrderBySequenceOrder(UUID mealPlanDayId);

    Optional<Meal> findByIdAndMealPlanDayId(UUID id, UUID mealPlanDayId);
}
