package com.nutricoach.messaging.mapper;

import com.nutricoach.messaging.dto.MessageResponse;
import com.nutricoach.messaging.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(target = "read", expression = "java(message.getReadAt() != null)")
    @Mapping(target = "sentAt", source = "createdAt")
    MessageResponse toResponse(Message message);
}
