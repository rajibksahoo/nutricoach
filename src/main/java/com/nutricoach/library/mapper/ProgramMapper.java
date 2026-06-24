package com.nutricoach.library.mapper;

import com.nutricoach.library.dto.ProgramSummaryResponse;
import com.nutricoach.library.entity.Program;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProgramMapper {

    @Mapping(target = "coverImageUrl", source = "coverImageUrl")
    ProgramSummaryResponse toSummary(Program program, String coverImageUrl);
}
