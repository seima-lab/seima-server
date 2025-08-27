package vn.fpt.seima.seimaserver.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.Budget;
import vn.fpt.seima.seimaserver.entity.BudgetPeriod;
import vn.fpt.seima.seimaserver.entity.BudgetPeriodStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BudgetPeriodRepository extends JpaRepository<BudgetPeriod, Integer> {
    List<BudgetPeriod> findByBudget_BudgetId(Integer budgetId);

    @Query("SELECT bp FROM BudgetPeriod bp WHERE bp.budget = :budget AND bp.status = :status and bp.endDate <= :date order by  bp.periodIndex desc")
    Page<BudgetPeriod> getListBudgetPeriods(@Param("budget") Budget budget,
                                            @Param("status") BudgetPeriodStatus status,
                                            Pageable pageable, @Param("date") LocalDateTime date);

    @Query("SELECT bp FROM BudgetPeriod bp WHERE bp.budget.budgetId = :budgetId and :timeNow between bp.startDate and bp.endDate")
    List<BudgetPeriod> findByBudget_BudgetIdAndTime(Integer budgetId, LocalDateTime timeNow);

    @Query("SELECT bp FROM BudgetPeriod bp WHERE bp.budget = :budget AND bp.status = :status and bp.startDate >= :date " +
            "union  SELECT bp FROM BudgetPeriod bp WHERE bp.budget = :budget AND bp.status = :status and :date between  bp.startDate  and bp.endDate")
    List<BudgetPeriod> getListBudgetPeriodsFuture(@Param("budget") Budget budget,
                                                  @Param("status") BudgetPeriodStatus status,
                                                  @Param("date") LocalDateTime date);

    @Query("SELECT bp FROM BudgetPeriod bp WHERE bp.budget = :budget and :date >= bp.startDate and bp.status = :status ORDER BY bp.periodIndex DESC limit 1")
    List<BudgetPeriod> findLatestByStatus(@Param("status") BudgetPeriodStatus status,
                                          @Param("budget") Budget budget,
                                          @Param("date") LocalDateTime date);

    @Query("SELECT COUNT(bp) FROM BudgetPeriod bp " +
            "WHERE bp.budget = :budget " +
            "AND bp.status = :status " +
            "AND bp.remainingAmount < 0 and :date between bp.startDate and bp.endDate")
    Integer countNegativeRemaining(@Param("budget") Budget budget,
                                   @Param("status") BudgetPeriodStatus status,
                                   @Param("date") LocalDateTime date);
}