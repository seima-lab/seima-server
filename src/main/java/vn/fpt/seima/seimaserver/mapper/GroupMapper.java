package vn.fpt.seima.seimaserver.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupResponse;
import vn.fpt.seima.seimaserver.entity.Group;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GroupMapper {
    GroupMapper INSTANCE = Mappers.getMapper(GroupMapper.class);

    @Mapping(target = "groupId", ignore = true)
    @Mapping(target = "groupCreatedDate", ignore = true)
    @Mapping(target = "groupIsActive", constant = "true")
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    Group toEntity(CreateGroupRequest request);

    GroupResponse toResponse(Group group);
} 