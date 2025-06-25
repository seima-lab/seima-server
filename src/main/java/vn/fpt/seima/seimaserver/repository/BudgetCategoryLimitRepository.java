package vn.fpt.seima.seimaserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.BudgetCategoryLimit;

@Repository
public interface BudgetCategoryLimitRepository extends JpaRepository<BudgetCategoryLimit, Integer> {
    void deleteByBudget_BudgetId(Integer budgetId);
} 