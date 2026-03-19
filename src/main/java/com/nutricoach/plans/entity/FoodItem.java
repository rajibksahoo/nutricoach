package com.nutricoach.plans.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "food_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodItem extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "name_hindi", length = 200)
    private String nameHindi;

    @Column(name = "name_regional", length = 200)
    private String nameRegional;

    @Enumerated(EnumType.STRING)
    @Column(name = "cuisine_type", length = 30)
    private CuisineType cuisineType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Column(name = "calories_per_100g", nullable = false, precision = 7, scale = 2)
    private BigDecimal caloriesPer100g;

    @Builder.Default
    @Column(name = "protein_per_100g", nullable = false, precision = 6, scale = 2)
    private BigDecimal proteinPer100g = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "carbs_per_100g", nullable = false, precision = 6, scale = 2)
    private BigDecimal carbsPer100g = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "fat_per_100g", nullable = false, precision = 6, scale = 2)
    private BigDecimal fatPer100g = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "fiber_per_100g", nullable = false, precision = 6, scale = 2)
    private BigDecimal fiberPer100g = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Source source = Source.IFCT;

    public enum CuisineType {
        SOUTH_INDIAN, GUJARATI, PUNJABI, JAIN, BENGALI, RAJASTHANI, MAHARASHTRIAN, NORTH_EAST, PAN_INDIAN
    }

    public enum Category {
        GRAIN, PROTEIN, VEGETABLE, FRUIT, DAIRY, FAT, BEVERAGE, SPICE, OTHER
    }

    public enum Source { IFCT, CUSTOM }
}
