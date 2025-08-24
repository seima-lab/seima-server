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
}
