package vn.fpt.seima.seimaserver.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.fpt.seima.seimaserver.dto.response.budgetPeriod.BudgetPeriodResponse;
import vn.fpt.seima.seimaserver.entity.Budget;
import vn.fpt.seima.seimaserver.entity.BudgetPeriod;

import java.util.List;

public interface BudgetPeriodService {
    List<BudgetPeriod> generateBudgetPeriods(Budget budget);
    Page<BudgetPeriodResponse> getListBudgetPeriods(Integer budgetId, Pageable pageable);

}
