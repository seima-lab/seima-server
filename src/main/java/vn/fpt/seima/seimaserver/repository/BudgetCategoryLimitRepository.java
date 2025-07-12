package vn.fpt.seima.seimaserver.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.fpt.seima.seimaserver.entity.BudgetCategoryLimit;

import java.util.List;

@Repository
public interface BudgetCategoryLimitRepository extends JpaRepository<BudgetCategoryLimit, Integer> {
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM budget_category_limit WHERE budget_id = :budgetId", nativeQuery = true)
    void deleteBudgetCategoryLimitByBudget(@Param("budgetId") Integer budgetId);

    @Query(value = "SELECT * FROM budget_category_limit WHERE  category_id = :categoryId", nativeQuery = true)
    List<BudgetCategoryLimit> findByTransaction(@Param("categoryId") Integer categoryId);
} 