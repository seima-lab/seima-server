package vn.fpt.seima.seimaserver.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.Budget;

import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Integer> {
    boolean existsByBudgetName(String budgetName);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId")
    List<Budget> findByUserId(@Param("userId") Integer userId);
}