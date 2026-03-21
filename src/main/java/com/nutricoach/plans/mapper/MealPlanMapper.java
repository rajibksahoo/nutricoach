package com.nutricoach.plans.mapper;

import com.nutricoach.plans.dto.MealPlanSummaryResponse;
import com.nutricoach.plans.entity.MealPlan;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MealPlanMapper {

    MealPlanSummaryResponse toSummary(MealPlan mealPlan);
}
