package com.nutricoach.coach.mapper;

import com.nutricoach.coach.dto.CoachResponse;
import com.nutricoach.coach.entity.Coach;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CoachMapper {

    CoachResponse toResponse(Coach coach);
}
