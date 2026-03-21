package com.nutricoach.plans.service;

import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.plans.dto.FoodItemResponse;
import com.nutricoach.plans.entity.FoodItem;
import com.nutricoach.plans.mapper.FoodItemMapper;
import com.nutricoach.plans.repository.FoodItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FoodItemService {

    private final FoodItemRepository foodItemRepository;
    private final FoodItemMapper foodItemMapper;

    @Transactional(readOnly = true)
    public List<FoodItemResponse> search(String query, FoodItem.CuisineType cuisine, FoodItem.Category category) {
        String q = StringUtils.hasText(query) ? query.trim() : null;
        return foodItemRepository.search(q, cuisine, category)
                .stream()
                .map(foodItemMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FoodItemResponse findById(UUID id) {
        return foodItemRepository.findById(id)
                .map(foodItemMapper::toResponse)
                .orElseThrow(() -> NutriCoachException.notFound("Food item not found"));
    }
}
