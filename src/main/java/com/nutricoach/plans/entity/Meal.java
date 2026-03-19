package com.nutricoach.plans.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "meals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meal extends BaseEntity {

    @Column(name = "meal_plan_day_id", nullable = false)
    private UUID mealPlanDayId;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "time_of_day")
    private LocalTime timeOfDay;

    @Builder.Default
    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder = 0;

    public enum MealType { BREAKFAST, LUNCH, DINNER, SNACK, PRE_WORKOUT, POST_WORKOUT }
}
