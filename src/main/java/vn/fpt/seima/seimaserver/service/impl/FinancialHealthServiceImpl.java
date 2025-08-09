package vn.fpt.seima.seimaserver.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.response.budget.FinancialHealthResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.repository.*;
import vn.fpt.seima.seimaserver.service.*;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialHealthServiceImpl implements FinancialHealthService {
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final FcmService fcmService;
    private final UserDeviceRepository userDeviceRepository;
    private final NotificationRepository notificationRepository;
    private final RedisService redisService;
    private static final String TITLE = "Financial Health Notification";
    private static final String MESSAGE = "Your financial health is currently low. Consider reviewing your expenses.";
    private final ObjectMapper objectMapper;
    private final BudgetPeriodRepository budgetPeriodRepository;

    /**
     * @return FinancialHealthResponse
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
            if (savingScore > 40) {
                savingScore = 40;
            } else if (savingScore < 0) {
                savingScore = 0;
            }
        }
        log.info("savingScore : {}", savingScore);
        List<Budget> budgets = budgetRepository.findByUserId(currentUser.getUserId());
        int budgetScore = 0;

        for (Budget budget : budgets) {
            int count = budgetPeriodRepository.countNegativeRemaining(budget, BudgetPeriodStatus.ACTIVE);
            if (count > 0) {
                budgetScore++;
            }
        }

        budgetScore = 30 - (int) (((double) budgetScore / budgets.size()) * 30);
        if (budgetScore > 30) {
            budgetScore = 30;
        } else if (budgetScore < 0) {
            budgetScore = 0;
        }
        log.info("budgetScore : {}", budgetScore);

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
        } else if (expected.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal expectedAbs = expected.abs();
            diffPercent = actual.subtract(expected)
                    .divide(expectedAbs, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
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
        log.info("assetScore : {}", assetScore);

        int finalScore = savingScore + budgetScore + assetScore;
        List<Integer> userIds = Collections.singletonList(currentUser.getUserId());
        List<String> fcmTokens = userDeviceRepository.findFcmTokensByUserIds(userIds);

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
        saveToRedisAndNotifyIfChanged(currentUser, finalScore, level, fcmTokens);
        return new FinancialHealthResponse(finalScore, level, LocalDateTime.now());
    }
    private void saveToRedisAndNotifyIfChanged(User currentUser, int finalScore, String level, List<String> fcmTokens) {
        String redisKey = "financial_health:" + currentUser.getUserId();
        Object rawData = redisService.get(redisKey);

        FinancialHealthResponse oldData = null;
        if (rawData != null) {
            oldData = objectMapper.convertValue(rawData, FinancialHealthResponse.class);
        }
        boolean levelChanged = false;
        String prevLevel = null;
        if (oldData != null ) {
            prevLevel =  oldData.getLevel();
            if (!prevLevel.equals(level)) {
                levelChanged = true;
            }
        }

        if (levelChanged) {
            Map<String, String> data = Map.of(
                    "type", "financial_health_notification",
                    "senderUserId", currentUser.getUserId().toString(),
                    "senderName", currentUser.getUserFullName()
            );

            fcmService.sendMulticastNotification(fcmTokens, TITLE, MESSAGE, data);

            Notification notification = new Notification();
            notification.setMessage(MESSAGE);
            notification.setReceiver(currentUser);
            notification.setNotificationType(NotificationType.FINANCIAL_HEALTH_LOW);
            notification.setTitle(TITLE);
            notification.setSender(currentUser);
            notificationRepository.save(notification);
        }
        Map<String, String> redisData = new HashMap<>();
        redisData.put("score", String.valueOf(finalScore));
        redisData.put("level", level);
        redisData.put("updatedAt", LocalDateTime.now().toString());

        redisService.set(redisKey, redisData);
        redisService.setTimeToLive(redisKey, Duration.ofDays(30).toDays());
    }

}
