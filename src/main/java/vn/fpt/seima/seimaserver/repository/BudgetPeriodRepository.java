package vn.fpt.seima.seimaserver.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.Budget;
import vn.fpt.seima.seimaserver.entity.BudgetPeriod;

import java.util.List;

@Repository
public interface BudgetPeriodRepository extends JpaRepository<BudgetPeriod, Integer> {
    List<BudgetPeriod> findByBudget_BudgetId(Integer budgetId);

    @Query("SELECT bp FROM BudgetPeriod bp WHERE bp.budget = :budget")
    Page<BudgetPeriod> getListBudgetPeriods(Budget budget, Pageable pageable);
}