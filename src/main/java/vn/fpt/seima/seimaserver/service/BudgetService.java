package vn.fpt.seima.seimaserver.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.fpt.seima.seimaserver.dto.budget.BudgetResponse;
import vn.fpt.seima.seimaserver.dto.budget.CreateBudgetRequest;
import vn.fpt.seima.seimaserver.dto.wallet.request.CreateWalletRequest;

public interface BudgetService {
    Page<BudgetResponse> getAllBudget(Pageable pageable);

    BudgetResponse getBudgetById(int id);

    BudgetResponse saveBudget(CreateBudgetRequest request);

    BudgetResponse updateBudget(Integer id,CreateBudgetRequest budget);

    void deleteBudget(int id);
} 