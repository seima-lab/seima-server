package vn.fpt.seima.seimaserver.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import vn.fpt.seima.seimaserver.entity.BudgetWallet;

import java.util.List;

public interface BudgetWalletRepository extends JpaRepository<BudgetWallet, Integer> {
    @Modifying
    @Query(value = "DELETE FROM budget_wallet WHERE budget_id = :budgetId", nativeQuery = true)
    void deleteBudgetWalletByBudget(@Param("budgetId") Integer budgetId);

    @Modifying
    @Query(value = "DELETE FROM budget_wallet WHERE wallet_id = :walletId", nativeQuery = true)
    void deleteBudgetWalletByWallet(@Param("walletId") Integer walletId);

    @Query("SELECT bw FROM BudgetWallet bw JOIN FETCH bw.budget WHERE bw.wallet.id = :walletId")
    List<BudgetWallet> findBudgetWalletsByWalletId(@Param("walletId") Integer walletId);

    @Modifying
    @Query(value = "SELECT  * FROM budget_wallet WHERE wallet_id = :walletId", nativeQuery = true)
    List<BudgetWallet> getBudgetWalletByWallet(@Param("walletId") Integer walletId);

    @Query("SELECT CASE WHEN COUNT(bw) > 0 THEN true ELSE false END " +
            "FROM BudgetWallet bw " +
            "WHERE bw.wallet.id = :walletId AND bw.budget.budgetId = :budgetId")
    boolean existsBudgetWalletByWalletAndBudget(@Param("budgetId") Integer budgetId,
                                                @Param("walletId") Integer walletId);
}
