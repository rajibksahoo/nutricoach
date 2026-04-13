package com.nutricoach.library.mapper;

import com.nutricoach.library.dto.ExerciseResponse;
import com.nutricoach.library.entity.Exercise;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExerciseMapper {

    ExerciseResponse toResponse(Exercise exercise);
}
