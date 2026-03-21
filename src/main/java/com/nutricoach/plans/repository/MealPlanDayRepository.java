package com.nutricoach.plans.repository;

import com.nutricoach.plans.entity.MealPlanDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealPlanDayRepository extends JpaRepository<MealPlanDay, UUID> {

    List<MealPlanDay> findByMealPlanIdOrderByDayNumber(UUID mealPlanId);

    Optional<MealPlanDay> findByIdAndMealPlanId(UUID id, UUID mealPlanId);

    boolean existsByMealPlanIdAndDayNumber(UUID mealPlanId, int dayNumber);
}
