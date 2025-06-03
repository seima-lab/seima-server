package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.entity.Budget;
import vn.fpt.seima.seimaserver.repository.BudgetRepository;
import vn.fpt.seima.seimaserver.service.BudgetService;

@Service
@AllArgsConstructor
public class BudgetServiceImpl implements BudgetService {
    private BudgetRepository budgetRepository;


    @Override
    public Page<Budget> getAllBudget(Pageable pageable) {
        return budgetRepository.findAll(pageable);
    }

    @Override
    public Budget getBudgetById(int id) {
        return budgetRepository.findById(id).orElse(null);
    }

    @Override
    public Budget saveBudget(Budget budget) {
        return budgetRepository.save(budget);
    }

    @Override
    public Budget updateBudget(Integer id,Budget budget) {
        return budgetRepository.save(budget);
    }

    @Override
    public void deleteBudget(int id) {
        budgetRepository.deleteById(id);
    }
}