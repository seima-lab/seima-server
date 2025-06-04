package vn.fpt.seima.seimaserver.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import vn.fpt.seima.seimaserver.dto.budget.BudgetResponse;
import vn.fpt.seima.seimaserver.dto.budget.CreateBudgetRequest;
import vn.fpt.seima.seimaserver.entity.Budget;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)

public interface BudgetMapper {
    BudgetMapper INSTANCE = Mappers.getMapper(BudgetMapper.class);

    Budget toEntity(CreateBudgetRequest request);

    BudgetResponse toResponse(Budget budget);

    @Mapping(target = "budgetId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateBudgetFromDto(CreateBudgetRequest dto, @MappingTarget Budget budget);
}
