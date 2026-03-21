package com.nutricoach.plans.mapper;

import com.nutricoach.plans.dto.FoodItemResponse;
import com.nutricoach.plans.entity.FoodItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FoodItemMapper {

    FoodItemResponse toResponse(FoodItem foodItem);
}
