package com.nutricoach.progress.mapper;

import com.nutricoach.progress.dto.CheckInResponse;
import com.nutricoach.progress.dto.ProgressLogResponse;
import com.nutricoach.progress.entity.CheckIn;
import com.nutricoach.progress.entity.ProgressLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProgressMapper {

    ProgressLogResponse toResponse(ProgressLog log);

    CheckInResponse toResponse(CheckIn checkIn);
}
