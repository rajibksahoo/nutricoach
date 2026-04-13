package com.nutricoach.library.mapper;

import com.nutricoach.library.dto.WorkoutSummaryResponse;
import com.nutricoach.library.entity.Workout;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkoutMapper {

    WorkoutSummaryResponse toSummary(Workout workout);
}
