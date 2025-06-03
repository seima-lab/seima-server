package vn.fpt.seima.seimaserver.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.fpt.seima.seimaserver.entity.Budget;

public interface BudgetService {
    Page<Budget> getAllBudget(Pageable pageable);

    Budget getBudgetById(int id);

    Budget saveBudget(Budget budget);

    Budget updateBudget(Integer id,Budget budget);

    void deleteBudget(int id);
} 