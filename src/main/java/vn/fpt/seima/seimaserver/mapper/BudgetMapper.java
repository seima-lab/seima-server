package vn.fpt.seima.seimaserver.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import vn.fpt.seima.seimaserver.dto.request.budget.CreateBudgetRequest;
import vn.fpt.seima.seimaserver.dto.response.budget.BudgetResponse;
import vn.fpt.seima.seimaserver.dto.response.category.CategoryResponse;
import vn.fpt.seima.seimaserver.entity.Budget;
import vn.fpt.seima.seimaserver.entity.BudgetCategoryLimit;
import vn.fpt.seima.seimaserver.entity.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BudgetMapper {
    BudgetMapper INSTANCE = Mappers.getMapper(BudgetMapper.class);

    Budget toEntity(CreateBudgetRequest request);

    @Mapping(target = "categories", expression = "java(mapCategories(budget.getBudgetCategoryLimits()))")
    BudgetResponse toResponse(Budget budget);

    @Mapping(target = "budgetId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateBudgetFromDto(CreateBudgetRequest dto, @MappingTarget Budget budget);

    default List<CategoryResponse> mapCategories(Set<BudgetCategoryLimit> limits) {
        if (limits == null) return new ArrayList<>();
        return limits.stream()
                .map(limit -> {
                    Category cat = limit.getCategory();
                    if (cat == null) return null;
                    CategoryResponse res = new CategoryResponse();
                    res.setCategoryId(cat.getCategoryId());
                    res.setCategoryName(cat.getCategoryName());
                    res.setCategoryIconUrl(cat.getCategoryIconUrl());
                    return res;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
