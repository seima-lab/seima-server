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
    private final WalletRepository walletRepository;

    /**
     * @return FinancialHealthResponse
     */
    @Override
    public FinancialHealthResponse calculateScore() {
        int finalScore = 0;
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("User must not be null");
        }

        String redisKey = "financial_health:" + currentUser.getUserId();
        Object cached = redisService.get(redisKey);

        if (cached != null) {
            FinancialHealthResponse cachedResponse = objectMapper.convertValue(cached, FinancialHealthResponse.class);
            log.info(" Get FinancialHealthResponse from Redis: {}", cachedResponse);
            return cachedResponse;
        }

        LocalDate dateFrom = LocalDate.now().withDayOfMonth(1);
        LocalDate dateFromPast = LocalDate.now()
                .minusMonths(1)
                .withDayOfMonth(1);
        LocalDate dateTo = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        boolean exists = transactionRepository.existsExpensesByUserAndDateRange(
                currentUser.getUserId(),
                dateFromPast.atStartOfDay(),
                dateTo.atTime(23, 59, 59));

        if (!exists) {
            finalScore = 100;
        }
        else{

            FinancialHealthResponse.IncomeExpenseSummary incomeExpense =
                    transactionRepository.findIncomeAndExpense(currentUser.getUserId(),
                            dateFrom.atStartOfDay(),
                            dateTo.atTime(23, 59, 59));

            BigDecimal totalIncome = incomeExpense != null && incomeExpense.getIncome() != null
                    ? incomeExpense.getIncome() : BigDecimal.ZERO;
            BigDecimal totalExpense = incomeExpense != null && incomeExpense.getExpense() != null
                    ? incomeExpense.getExpense() : BigDecimal.ZERO;

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


            FinancialHealthResponse.IncomeExpenseSummary incomeExpensePrev =
                    transactionRepository.findIncomeAndExpense(currentUser.getUserId(),
                            prevMonthStart.atStartOfDay(),
                            prevMonthEnd.atTime(23, 59, 59));

            BigDecimal prevIncome = incomeExpensePrev != null && incomeExpensePrev.getIncome() != null
                    ? incomeExpensePrev.getIncome() : BigDecimal.ZERO;
            BigDecimal prevExpense = incomeExpensePrev != null && incomeExpensePrev.getExpense() != null
                    ? incomeExpensePrev.getExpense() : BigDecimal.ZERO;

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
            finalScore = savingScore + budgetScore + assetScore;

        }

        List<Integer> userIds = Collections.singletonList(currentUser.getUserId());
        List<String> fcmTokens = userDeviceRepository.findFcmTokensByUserIds(userIds);
        String level;
        if (finalScore >= 75) {
            level = "Very Good";
        } else if (finalScore >= 40) {
            level = "Good";
        } else if (finalScore >= 20) {
            level = "Medium";
        } else {
            level = "Low";
        }
        LocalDateTime updateAt = LocalDateTime.now();
        BigDecimal balance = walletRepository.sumBalanceByUserId(currentUser.getUserId());

        saveToRedisAndNotifyIfChanged(currentUser, finalScore,balance ,level, fcmTokens, updateAt);
        return  FinancialHealthResponse.builder()
                .score(finalScore)
                .level(level)
                .updatedAt(updateAt)
                .balance(balance)
                .build();
    }
    private void saveToRedisAndNotifyIfChanged(User currentUser,
                                               int finalScore,
                                               BigDecimal balance,
                                               String level,
                                               List<String> fcmTokens,
                                               LocalDateTime updateAt) {
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
        redisData.put("updatedAt", updateAt.toString());
        redisData.put("balance", String.valueOf(balance));
        redisService.set(redisKey, redisData);
        redisService.setTimeToLive(redisKey, Duration.ofDays(3600*24).toDays());
    }

}
