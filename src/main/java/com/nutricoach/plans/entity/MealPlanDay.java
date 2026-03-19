package com.nutricoach.plans.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "meal_plan_days")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlanDay extends BaseEntity {

    @Column(name = "meal_plan_id", nullable = false)
    private UUID mealPlanId;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(name = "total_calories")
    private Integer totalCalories;

    @Column(name = "total_protein_g", precision = 6, scale = 2)
    private BigDecimal totalProteinG;

    @Column(name = "total_carbs_g", precision = 6, scale = 2)
    private BigDecimal totalCarbsG;

    @Column(name = "total_fat_g", precision = 6, scale = 2)
    private BigDecimal totalFatG;
}
