package vn.fpt.seima.seimaserver.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.fpt.seima.seimaserver.dto.budget.BudgetResponse;
import vn.fpt.seima.seimaserver.dto.budget.CreateBudgetRequest;
import vn.fpt.seima.seimaserver.dto.budgetCategoryLimit.BudgetCategoryLimitResponse;
import vn.fpt.seima.seimaserver.dto.budgetCategoryLimit.CreateBudgetCategoryLimit;

public interface BudgetCategoryLimitService {

    Page<BudgetCategoryLimitResponse> getAllBudgetCategoryLimit(Pageable pageable);

    BudgetCategoryLimitResponse getBudgetCategoryLimitById(int id);

    BudgetCategoryLimitResponse saveBudgetCategoryLimit(CreateBudgetCategoryLimit request);

    BudgetCategoryLimitResponse updateBudgetCategoryLimit(Integer id,CreateBudgetCategoryLimit budget);

    void deleteBudgetCategoryLimit(int id);
} 