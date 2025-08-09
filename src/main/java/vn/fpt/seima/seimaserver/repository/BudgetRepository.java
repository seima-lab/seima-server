package vn.fpt.seima.seimaserver.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.Budget;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Integer> {
    boolean existsByBudgetName(String budgetName);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId")
    List<Budget> findByUserId(@Param("userId") Integer userId);

    @Query("select case when count(*) < 5 then true else false end from Budget b where b.user.id = :userId ")
    boolean countBudgetByUserId(@Param("userId") Integer userId);

    Page<Budget> findByUser_UserId(Integer userId, Pageable pageable);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.budgetName LIKE %:budgetName%")
    Page<Budget> findByUser_UserIdAndBudgetNameContaining(@Param("userId") Integer userId, @Param("budgetName") String budgetName, Pageable pageable);

    @Modifying
    @Query(value = "DELETE FROM budget WHERE budget_id = :budgetId", nativeQuery = true)
    void deleteBudget(@Param("budgetId") Integer budgetId);

    @Query("SELECT DISTINCT b FROM Budget b JOIN b.budgetCategoryLimits bcl " +
            "WHERE b.user.userId = :userId AND bcl.category.categoryId IN :categoryIds")
    Page<Budget> findByUserIdAndCategoryIds(@Param("userId") Integer userId,
                                            @Param("categoryIds") List<Integer> categoryIds,
                                            Pageable pageable);
}