package com.nutricoach.library.mapper;

import com.nutricoach.library.dto.ProgramSummaryResponse;
import com.nutricoach.library.entity.Program;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProgramMapper {

    ProgramSummaryResponse toSummary(Program program);
}
