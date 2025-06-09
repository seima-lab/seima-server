package vn.fpt.seima.seimaserver.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.fpt.seima.seimaserver.dto.response.budgetCategoryLimit.BudgetCategoryLimitResponse;
import vn.fpt.seima.seimaserver.dto.request.budgetCategory.CreateBudgetCategoryLimitRequest;

public interface BudgetCategoryLimitService {

    Page<BudgetCategoryLimitResponse> getAllBudgetCategoryLimit(Pageable pageable);

    BudgetCategoryLimitResponse getBudgetCategoryLimitById(int id);

    BudgetCategoryLimitResponse saveBudgetCategoryLimit(CreateBudgetCategoryLimitRequest request);

    BudgetCategoryLimitResponse updateBudgetCategoryLimit(Integer id, CreateBudgetCategoryLimitRequest budget);

    void deleteBudgetCategoryLimit(int id);
} 