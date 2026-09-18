package com.nutricoach.library.mapper;

import com.nutricoach.library.dto.ProgramSummaryResponse;
import com.nutricoach.library.entity.Program;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProgramMapper {

    @Mapping(target = "coverImageUrl", source = "coverImageUrl")
    @Mapping(target = "equipment", source = "equipment")
    // Lombok names the getter isTemplate(), so the bean property is "template"
    // while the DTO component is "isTemplate". Without this they do not match
    // and MapStruct silently maps false.
    @Mapping(target = "isTemplate", source = "program.template")
    ProgramSummaryResponse toSummary(Program program, String coverImageUrl, List<String> equipment);
}
