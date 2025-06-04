package vn.fpt.seima.seimaserver.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import vn.fpt.seima.seimaserver.dto.budgetCategoryLimit.BudgetCategoryLimitResponse;
import vn.fpt.seima.seimaserver.dto.budgetCategoryLimit.CreateBudgetCategoryLimit;
import vn.fpt.seima.seimaserver.entity.BudgetCategoryLimit;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BudgetCategoryLimitMapper {

    BudgetCategoryLimit toEntity(CreateBudgetCategoryLimit request);

    BudgetCategoryLimitResponse toResponse(BudgetCategoryLimit budget);

    @Mapping(target = "budget_category_limit_id", ignore = true)
    void updateBudgetFromDto(CreateBudgetCategoryLimit dto, @MappingTarget BudgetCategoryLimit budget);
}
