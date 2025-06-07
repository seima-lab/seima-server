package vn.fpt.seima.seimaserver.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import vn.fpt.seima.seimaserver.dto.request.category.CreateCategoryRequest;
import vn.fpt.seima.seimaserver.dto.response.category.CategoryResponse;
import vn.fpt.seima.seimaserver.entity.Category;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)

public interface CategoryMapper {
    CategoryMapper INSTANCE = Mappers.getMapper(CategoryMapper.class);

    Category toEntity(CreateCategoryRequest request);

    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "group.groupId", target = "groupId")
    CategoryResponse toResponse(Category category);

    @Mapping(target = "categoryId", ignore = true)
    void updateCategoryFromDto(CreateCategoryRequest dto, @MappingTarget Category category);
}
