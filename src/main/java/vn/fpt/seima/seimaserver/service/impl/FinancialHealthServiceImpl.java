package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.response.budget.FinancialHealthResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.repository.BudgetRepository;
import vn.fpt.seima.seimaserver.repository.TransactionRepository;
import vn.fpt.seima.seimaserver.service.BudgetService;
import vn.fpt.seima.seimaserver.service.FinancialHealthService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialHealthServiceImpl implements FinancialHealthService {
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;


    /**
     * @return
     */
    @Override
    public FinancialHealthResponse calculateScore() {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        LocalDate dateFrom = LocalDate.now().withDayOfMonth(1);
        LocalDate dateTo = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        List<Transaction> transactions = transactionRepository.findExpensesByUserAndDateRange(currentUser.getUserId(),
                dateFrom.atStartOfDay(),
                dateTo.atTime(23, 59, 59));

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            if (transaction.getTransactionType() == TransactionType.EXPENSE) {
                totalExpense = totalExpense.add(transaction.getAmount());
            } else if (transaction.getTransactionType() == TransactionType.INCOME) {
                totalIncome = totalIncome.add(transaction.getAmount());
            }
        }

        int savingScore = 0;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal savingRate = (totalIncome.subtract(totalExpense))
                    .divide(totalIncome, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            savingScore = savingRate.intValue() * 40 / 100;
            if (savingScore >40) {
                savingScore = 40;
            }
        }
        List<Budget> budgets = budgetRepository.findByUserId(currentUser.getUserId());
        int compliantBudgets = 0;
        for (Budget budget : budgets) {
            for (BudgetCategoryLimit categoryLimit : budget.getBudgetCategoryLimits()) {
                BigDecimal actualSpent = transactionRepository.sumExpensesByCategoryAndMonth(
                        currentUser.getUserId(),
                        categoryLimit.getCategory().getCategoryId(),
                        dateFrom.atStartOfDay(),
                        dateTo.atTime(23, 59, 59)
                );

                BigDecimal limitAmount = budget.getOverallAmountLimit();

                if (actualSpent == null || actualSpent.compareTo(limitAmount) <= 0) {
                    compliantBudgets++;
                }
            }
        }
        int budgetScore = 0;
        if (!budgets.isEmpty()) {
            BigDecimal rate = BigDecimal.valueOf(compliantBudgets)
                    .divide(BigDecimal.valueOf(budgets.size()), 2, RoundingMode.HALF_UP);
            budgetScore = rate.multiply(BigDecimal.valueOf(30)).intValue();

            if (budgetScore > 30) {
                budgetScore = 30;
            }
        }

        // 3. So sánh với tháng trước
        LocalDate prevMonthStart = dateFrom.minusMonths(1);
        LocalDate prevMonthEnd = prevMonthStart.withDayOfMonth(prevMonthStart.lengthOfMonth());

        List<Transaction> lastMonthTxs = transactionRepository.findExpensesByUserAndDateRange(
                currentUser.getUserId(),
                prevMonthStart.atStartOfDay(),
                prevMonthEnd.atTime(23, 59, 59)
        );

        BigDecimal prevIncome = BigDecimal.ZERO;
        BigDecimal prevExpense = BigDecimal.ZERO;

        for (Transaction tx : lastMonthTxs) {
            if (tx.getTransactionType() == TransactionType.INCOME) {
                prevIncome = prevIncome.add(tx.getAmount());
            } else if (tx.getTransactionType() == TransactionType.EXPENSE) {
                prevExpense = prevExpense.add(tx.getAmount());
            }
        }

        BigDecimal actual = totalIncome.subtract(totalExpense);
        BigDecimal expected = prevIncome.subtract(prevExpense);

        BigDecimal diffPercent;
        if (expected.compareTo(BigDecimal.ZERO) == 0) {
            if (actual.compareTo(BigDecimal.ZERO) > 0) {
                diffPercent = BigDecimal.valueOf(999); // tăng cực mạnh
            } else {
                diffPercent = BigDecimal.ZERO;
            }
        } else {
            diffPercent = actual.subtract(expected)
                    .divide(expected, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        int assetScore = 0;

        if (diffPercent.compareTo(BigDecimal.valueOf(100)) >= 0) {
            assetScore = 30; // tăng >= 100%
        } else if (diffPercent.compareTo(BigDecimal.valueOf(50)) >= 0) {
            assetScore = 20; // tăng từ 50% đến < 100%
        } else if (diffPercent.compareTo(BigDecimal.ZERO) > 0) {
            assetScore = 10; // tăng < 50%
        }

        int finalScore = savingScore + budgetScore + assetScore;
        String level;
        if (finalScore >= 80) {
            level = "Very Good";
        } else if (finalScore >= 60) {
            level = "Good";
        } else if (finalScore >= 40) {
            level = "Medium";
        } else {
            level = "Low";
        }

        return new FinancialHealthResponse(finalScore, level);
    }
}
