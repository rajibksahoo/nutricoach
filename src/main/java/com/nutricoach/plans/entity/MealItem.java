package com.nutricoach.plans.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "meal_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealItem extends BaseEntity {

    @Column(name = "meal_id", nullable = false)
    private UUID mealId;

    @Column(name = "food_item_id", nullable = false)
    private UUID foodItemId;

    @Column(name = "quantity_grams", nullable = false, precision = 7, scale = 2)
    private BigDecimal quantityGrams;

    @Builder.Default
    @Column(name = "quantity_unit", nullable = false, length = 20)
    private String quantityUnit = "g";

    @Column
    private Integer calories;

    @Column(name = "protein_g", precision = 6, scale = 2)
    private BigDecimal proteinG;

    @Column(name = "carbs_g", precision = 6, scale = 2)
    private BigDecimal carbsG;

    @Column(name = "fat_g", precision = 6, scale = 2)
    private BigDecimal fatG;
}
