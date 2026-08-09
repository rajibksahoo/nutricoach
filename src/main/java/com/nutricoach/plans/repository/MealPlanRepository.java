package com.nutricoach.plans.repository;

import com.nutricoach.plans.entity.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealPlanRepository extends JpaRepository<MealPlan, UUID> {

    List<MealPlan> findByClientIdAndCoachIdAndDeletedAtIsNull(UUID clientId, UUID coachId);

    Optional<MealPlan> findByIdAndCoachIdAndDeletedAtIsNull(UUID id, UUID coachId);

    // Client portal — verifies plan belongs to both the client and coach
    Optional<MealPlan> findByIdAndClientIdAndCoachIdAndDeletedAtIsNull(UUID id, UUID clientId, UUID coachId);

    long countByCoachIdAndDeletedAtIsNull(UUID coachId);

    /** Plans of a given status ending inside a date window — dashboard "expiring soon". */
    List<MealPlan> findByCoachIdAndStatusAndDeletedAtIsNullAndEndDateBetween(
            UUID coachId, MealPlan.Status status, LocalDate from, LocalDate to);
}
