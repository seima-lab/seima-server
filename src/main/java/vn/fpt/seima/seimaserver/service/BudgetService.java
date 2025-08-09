package vn.fpt.seima.seimaserver.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.fpt.seima.seimaserver.dto.request.budget.CreateBudgetRequest;
import vn.fpt.seima.seimaserver.dto.request.budget.UpdateBudgetRequest;
import vn.fpt.seima.seimaserver.dto.response.budget.BudgetLastResponse;
import vn.fpt.seima.seimaserver.dto.response.budget.BudgetResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface BudgetService {
    Page<BudgetResponse> getAllBudget(Pageable pageable);

    BudgetResponse getBudgetById(int id);

    BudgetResponse saveBudget(CreateBudgetRequest request);

    BudgetResponse updateBudget(Integer id, UpdateBudgetRequest budget);

    void deleteBudget(int id);

    void reduceAmount(Integer userId, Integer categoryId, BigDecimal amount, LocalDateTime transactionDate, String type, String code);

    List<BudgetLastResponse> getLastBudget();

    Page<BudgetResponse> getBudgetByName(String budgetName, Pageable pageable);

    Page<BudgetResponse> getBudgetsByCategories(List<Integer> categoryIds, Pageable pageable);
}