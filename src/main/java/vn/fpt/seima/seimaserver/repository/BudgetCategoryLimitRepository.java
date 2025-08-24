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
    @Query(value = "DELETE FROM budget_category_limit WHERE budget_id = :budgetId", nativeQuery = true)
    void deleteBudgetCategoryLimitByBudget(@Param("budgetId") Integer budgetId);

    @Query(value = "SELECT * FROM budget_category_limit WHERE  category_id = :categoryId", nativeQuery = true)
    List<BudgetCategoryLimit> findByTransaction(@Param("categoryId") Integer categoryId);

    @Query(value = "SELECT * FROM budget_category_limit WHERE  budget_id = :budgetId", nativeQuery = true)
    List<BudgetCategoryLimit> findByBudget(@Param("budgetId") Integer budgetId);

    @Query("SELECT CASE WHEN COUNT(bc) > 0 THEN true ELSE false END " +
            "FROM BudgetCategoryLimit bc " +
            "WHERE bc.budget.budgetId = :budgetId AND bc.category.categoryId = :categoryId")
    boolean existsByBudgetIdAndCategoryId(@Param("budgetId") Integer budgetId,
                                          @Param("categoryId") Integer categoryId);

    void deleteByCategory_CategoryId(Integer categoryId);


} 