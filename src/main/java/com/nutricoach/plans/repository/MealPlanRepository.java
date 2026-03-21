package com.nutricoach.plans.repository;

import com.nutricoach.plans.entity.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealPlanRepository extends JpaRepository<MealPlan, UUID> {

    List<MealPlan> findByClientIdAndCoachIdAndDeletedAtIsNull(UUID clientId, UUID coachId);

    Optional<MealPlan> findByIdAndCoachIdAndDeletedAtIsNull(UUID id, UUID coachId);
}
