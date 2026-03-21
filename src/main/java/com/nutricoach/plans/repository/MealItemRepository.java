package com.nutricoach.plans.repository;

import com.nutricoach.plans.entity.MealItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealItemRepository extends JpaRepository<MealItem, UUID> {

    List<MealItem> findByMealId(UUID mealId);

    Optional<MealItem> findByIdAndMealId(UUID id, UUID mealId);

    // Load all items for an entire day in one query (used for nutrition recalculation)
    @Query("SELECT mi FROM MealItem mi WHERE mi.mealId IN " +
           "(SELECT m.id FROM Meal m WHERE m.mealPlanDayId = :dayId)")
    List<MealItem> findByDayId(@Param("dayId") UUID dayId);
}
