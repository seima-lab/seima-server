package vn.fpt.seima.seimaserver.mapper;

import org.mapstruct.Mapper;
import vn.fpt.seima.seimaserver.dto.response.budgetPeriod.BudgetPeriodResponse;
import vn.fpt.seima.seimaserver.entity.BudgetPeriod;

@Mapper(componentModel = "spring")
public interface BudgetPeriodMapper {
    BudgetPeriodResponse toResponse(BudgetPeriod entity);
}