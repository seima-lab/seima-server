package vn.fpt.seima.seimaserver.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.fpt.seima.seimaserver.dto.request.budget.CreateBudgetRequest;
import vn.fpt.seima.seimaserver.dto.response.budget.BudgetResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface BudgetService {
    Page<BudgetResponse> getAllBudget(Pageable pageable);

    BudgetResponse getBudgetById(int id);

    BudgetResponse saveBudget(CreateBudgetRequest request);

    BudgetResponse updateBudget(Integer id,CreateBudgetRequest budget);

    void deleteBudget(int id);

    void reduceAmount(Integer userId, Integer categoryId, BigDecimal amount, LocalDateTime transactionDate);
}