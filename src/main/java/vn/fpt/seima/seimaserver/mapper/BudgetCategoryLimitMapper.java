package vn.fpt.seima.seimaserver.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import vn.fpt.seima.seimaserver.dto.response.budgetCategoryLimit.BudgetCategoryLimitResponse;
import vn.fpt.seima.seimaserver.dto.request.budgetCategory.CreateBudgetCategoryLimitRequest;
import vn.fpt.seima.seimaserver.entity.BudgetCategoryLimit;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BudgetCategoryLimitMapper {

    BudgetCategoryLimit toEntity(CreateBudgetCategoryLimitRequest request);

    BudgetCategoryLimitResponse toResponse(BudgetCategoryLimit budget);

    @Mapping(target = "budgetCategoryLimitId", ignore = true)
    void updateBudgetFromDto(CreateBudgetCategoryLimitRequest dto, @MappingTarget BudgetCategoryLimit budget);
}
