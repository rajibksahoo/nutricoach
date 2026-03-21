package com.nutricoach.plans.repository;

import com.nutricoach.plans.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FoodItemRepository extends JpaRepository<FoodItem, UUID> {

    @Query("SELECT f FROM FoodItem f WHERE " +
           "(:query IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "   OR LOWER(f.nameHindi) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:cuisine IS NULL OR f.cuisineType = :cuisine) " +
           "AND (:category IS NULL OR f.category = :category) " +
           "ORDER BY f.name")
    List<FoodItem> search(
            @Param("query") String query,
            @Param("cuisine") FoodItem.CuisineType cuisine,
            @Param("category") FoodItem.Category category);
}
