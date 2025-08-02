package vn.fpt.seima.seimaserver.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.fpt.seima.seimaserver.dto.response.budget.BudgetLastResponse;
import vn.fpt.seima.seimaserver.dto.response.budgetPeriod.BudgetPeriodResponse;
import vn.fpt.seima.seimaserver.entity.BudgetPeriod;

@Mapper(componentModel = "spring")
public interface BudgetPeriodMapper {
    @Mapping(target = "status", source = "status")
    BudgetPeriodResponse toResponse(BudgetPeriod entity);
    @Mapping(target = "status", source = "status")
    BudgetLastResponse toLastResponse(BudgetPeriod entity);
}