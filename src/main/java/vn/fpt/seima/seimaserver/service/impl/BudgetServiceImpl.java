package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fpt.seima.seimaserver.dto.request.budget.CreateBudgetRequest;
import vn.fpt.seima.seimaserver.dto.request.budget.UpdateBudgetRequest;
import vn.fpt.seima.seimaserver.dto.response.budget.BudgetLastResponse;
import vn.fpt.seima.seimaserver.dto.response.budget.BudgetResponse;
import vn.fpt.seima.seimaserver.dto.response.category.CategoryResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.mapper.BudgetMapper;
import vn.fpt.seima.seimaserver.repository.*;
import vn.fpt.seima.seimaserver.service.BudgetCategoryLimitService;
import vn.fpt.seima.seimaserver.service.BudgetPeriodService;
import vn.fpt.seima.seimaserver.service.BudgetService;
import vn.fpt.seima.seimaserver.service.FcmService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j

public class BudgetServiceImpl implements BudgetService {
    private  BudgetRepository budgetRepository;
    private final BudgetMapper budgetMapper;
    private final BudgetCategoryLimitRepository budgetCategoryLimitRepository;
    private final BudgetPeriodRepository budgetPeriodRepository;
    private final BudgetPeriodService budgetPeriodService;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private FcmService fcmService;
    private UserDeviceRepository userDeviceRepository;
    private NotificationRepository notificationRepository;
    private static final String TITLE = "Budget Notification";
    private static final String MESSAGE = "Your spending has exceeded the set threshold. Please review your expenses.";

    @Override
    public Page<BudgetResponse> getAllBudget(Pageable pageable) {
        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        Page<Budget> budgets = budgetRepository.findByUser_UserId(user.getUserId(),pageable);

        return budgets.map(budgetMapper::toResponse);
    }

    @Override
    public BudgetResponse getBudgetById(int id) {
        Budget budget = budgetRepository.findById(id).orElseThrow(()
                -> new ResourceNotFoundException("Budget not found for this id :: " + id));
        return BudgetMapper.INSTANCE.toResponse(budget);
    }

    @Override
    @Transactional
    public BudgetResponse saveBudget(CreateBudgetRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }
        if (budgetRepository.existsByBudgetName(request.getBudgetName())) {
            throw new IllegalArgumentException("Budget name already exists");
        }
        if(request.getOverallAmountLimit().compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Overall amount limit must be greater than zero");
        }

        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }

        if (request.getCategoryList().isEmpty()) {
            throw new IllegalArgumentException("Category list must not be empty");
        }

        if (!budgetRepository.countBudgetByUserId(user.getUserId())) {
            throw new IllegalArgumentException("The user cannot have more than 5 budgets.");
        }
        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("Start date must not be null");
        }
        if (request.getEndDate() != null) {
            if (!request.getStartDate().isBefore(request.getEndDate())) {
                throw new IllegalArgumentException("Start date must be before end date");
            }
        }

        if (request.getEndDate() == null) {
            request.setEndDate(LocalDateTime.of(LocalDate.now().getYear(), 12, 31, 23, 59, 59));
        }
        List<Integer> categoryIds = new ArrayList<>();
        for (Category category : request.getCategoryList()){
            categoryIds.add(category.getCategoryId());
        }
        Budget budget = budgetMapper.toEntity(request);

        budget.setUser(user);
        Budget savedBudget = budgetRepository.save(budget);

        for (Category category : request.getCategoryList()) {
            BudgetCategoryLimit budgetCategoryLimit = new BudgetCategoryLimit();
            budgetCategoryLimit.setCategory(category);
            budgetCategoryLimit.setBudget(budget);

            budgetCategoryLimitRepository.save(budgetCategoryLimit);
        }
        List<Transaction> transactions = transactionRepository.listExpensesByCategoryAndMonth(
                user.getUserId(),
                categoryIds,
                request.getStartDate(),
                request.getEndDate()
        );
        List<BudgetPeriod> periods = budgetPeriodService.generateBudgetPeriods(savedBudget);
        for (Transaction transaction : transactions) {
            for (BudgetPeriod period : periods) {
                if (!transaction.getTransactionDate().isBefore(period.getStartDate()) &&
                        !transaction.getTransactionDate().isAfter(period.getEndDate())) {
                    period.setRemainingAmount(period.getRemainingAmount().subtract(transaction.getAmount()));
                }
            }
        }

        budgetPeriodRepository.saveAll(periods);
        return budgetMapper.toResponse(savedBudget);
    }

    @Override
    @Transactional
    public BudgetResponse updateBudget(Integer id, UpdateBudgetRequest request) {
        try{
            Budget existingBudget = budgetRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Budget not found for this id: " + id));

            User user = UserUtils.getCurrentUser();
            if (user == null) {
                throw new IllegalArgumentException("User must not be null");
            }

            if (request.getOverallAmountLimit().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Overall amount limit must be greater than zero");
            }

            if (request.getCategoryList() == null || request.getCategoryList().isEmpty()) {
                throw new IllegalArgumentException("Category list must not be empty");
            }

            List<Integer> categoryIds = new ArrayList<>();
            budgetCategoryLimitRepository.deleteBudgetCategoryLimitByBudget(existingBudget.getBudgetId());
            for (Category category : request.getCategoryList()) {
                BudgetCategoryLimit budgetCategoryLimit = new BudgetCategoryLimit();
                budgetCategoryLimit.setCategory(category);
                budgetCategoryLimit.setBudget(existingBudget);
                budgetCategoryLimitRepository.save(budgetCategoryLimit);
                categoryIds.add(category.getCategoryId());
            }
            Budget updatedBudget;
            if (request.isUpdateAmount()) {
                LocalDateTime now = LocalDateTime.now();
                List<BudgetPeriod> budgetPeriods = budgetPeriodRepository.getListBudgetPeriodsFuture(
                        existingBudget, BudgetPeriodStatus.ACTIVE, now);

                for (BudgetPeriod budgetPeriod : budgetPeriods) {
                    budgetPeriod.setAmountLimit(request.getOverallAmountLimit());
                    budgetPeriod.setRemainingAmount(request.getOverallAmountLimit());
                }

                List<Transaction> transactions = transactionRepository.listExpensesByCategoryAndMonth(
                        user.getUserId(), categoryIds, existingBudget.getStartDate(), existingBudget.getEndDate());

                for (Transaction transaction : transactions) {
                    for (BudgetPeriod period : budgetPeriods) {
                        if (!transaction.getTransactionDate().isBefore(period.getStartDate())
                                && !transaction.getTransactionDate().isAfter(period.getEndDate())) {
                            period.setRemainingAmount(period.getRemainingAmount().subtract(transaction.getAmount()));
                        }
                    }
                }

                budgetPeriodRepository.saveAll(budgetPeriods);
                existingBudget.setOverallAmountLimit(request.getOverallAmountLimit());
                existingBudget.setBudgetRemainingAmount(request.getBudgetRemainingAmount());

                updatedBudget = existingBudget;
            } else {
                if (budgetRepository.existsByBudgetName(request.getBudgetName())
                        && !existingBudget.getBudgetName().equals(request.getBudgetName())) {
                    throw new IllegalArgumentException("Budget name already exists");
                }

                if (request.getStartDate() == null) {
                    throw new IllegalArgumentException("Start date must not be null");
                }
                if (request.getEndDate() != null) {
                    if (!request.getStartDate().isBefore(request.getEndDate())) {
                        throw new IllegalArgumentException("Start date must be before end date");
                    }
                }

                if (request.getEndDate() == null) {
                    request.setEndDate(LocalDateTime.of(LocalDate.now().getYear(), 12, 31, 23, 59, 59));
                }

                budgetPeriodRepository.deleteAll(budgetPeriodRepository.findByBudget_BudgetId(existingBudget.getBudgetId()));

                budgetMapper.updateBudgetFromDto(request, existingBudget);
                existingBudget.setUser(user);
                updatedBudget = budgetRepository.save(existingBudget);

                List<BudgetPeriod> periods = budgetPeriodService.generateBudgetPeriods(updatedBudget);
                List<Transaction> transactions = transactionRepository.listExpensesByCategoryAndMonth(
                        user.getUserId(), categoryIds, request.getStartDate(), request.getEndDate());

                for (Transaction transaction : transactions) {
                    for (BudgetPeriod period : periods) {
                        if (!transaction.getTransactionDate().isBefore(period.getStartDate())
                                && !transaction.getTransactionDate().isAfter(period.getEndDate())) {
                            period.setRemainingAmount(period.getRemainingAmount().subtract(transaction.getAmount()));
                        }
                    }
                }

                budgetPeriodRepository.saveAll(periods);
            }

            return budgetMapper.toResponse(updatedBudget);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    @Transactional
    public void deleteBudget(int id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found for this id: " + id));

        budgetCategoryLimitRepository.deleteBudgetCategoryLimitByBudget(budget.getBudgetId());
        budgetPeriodRepository.deleteAll(budgetPeriodRepository.findByBudget_BudgetId(budget.getBudgetId()));
        budgetRepository.deleteBudget(id);
    }

    @Override
    @Transactional
    public void reduceAmount(Integer userId, Integer categoryId, BigDecimal amount,
                             LocalDateTime transactionDate, String type, String code) {
        String title = null;
        String message = null;
        NotificationType notificationType = null;
        List<Budget> existingBudget = budgetRepository.findByUserId(userId);
        User user = UserUtils.getCurrentUser();
        if (user == null || existingBudget.isEmpty()) return;

        List<String> fcmTokens = userDeviceRepository.findFcmTokensByUserIds(
                Collections.singletonList(user.getUserId())
        );

        for (Budget budget : existingBudget) {
            List<BudgetCategoryLimit> budgetCategoryLimits =
                    budgetCategoryLimitRepository.findByTransaction(categoryId);
            if (budgetCategoryLimits.isEmpty()) continue;

            List<BudgetPeriod> budgetPeriods =
                    budgetPeriodRepository.findByBudget_BudgetId(budget.getBudgetId());

            for (BudgetPeriod budgetPeriod : budgetPeriods) {
                boolean inRange = !transactionDate.isBefore(budgetPeriod.getStartDate())
                        && !transactionDate.isAfter(budgetPeriod.getEndDate());
                if (!inRange) continue;

                switch (type) {
                    case "EXPENSE": {
                        BigDecimal newAmount = budgetPeriod.getRemainingAmount().subtract(amount);
                        budgetPeriod.setRemainingAmount(newAmount);
                        break;
                    }
                    case "INCOME": {
                        break;
                    }
                    case "update-subtract": {
                        BigDecimal newAmount = budgetPeriod.getRemainingAmount().subtract(amount);
                        budgetPeriod.setRemainingAmount(newAmount);
                        break;
                    }
                    case "update-add": {
                        BigDecimal newAmount = budgetPeriod.getRemainingAmount().add(amount);
                        budgetPeriod.setRemainingAmount(newAmount);
                        break;
                    }
                    default:
                        break;
                }

                BigDecimal remaining = budgetPeriod.getRemainingAmount();
                BigDecimal limit = budgetPeriod.getAmountLimit();
                if(type.equals("EXPENSE")) {
                    if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                        title = "Budget Exceeded!";
                        message = "You have exceeded your budget limit.";
                        notificationType = NotificationType.BUDGET_LIMIT_EXCEEDED;
                    } else if (remaining.compareTo(BigDecimal.ZERO) == 0) {
                        title = "Budget Depleted!";
                        message = "You have fully used your budget for this period.";
                        notificationType = NotificationType.BUDGET_LIMIT_REACHED;
                    } else {
                        BigDecimal warningThreshold = limit.multiply(BigDecimal.valueOf(0.1));
                        if (remaining.compareTo(warningThreshold) < 0) {
                            title = "Budget Running Low!";
                            message = "Only 10% of your budget remains. Spend wisely.";
                            notificationType = NotificationType.BUDGET_LIMIT_WARNING;
                        }
                    }
                }

            }

            budgetPeriodRepository.saveAll(budgetPeriods);
        }
        if (title != null) {
            Map<String, String> data = Map.of(
                    "type", "budget_notification",
                    "senderUserId", userId.toString(),
                    "senderName", user.getUserFullName()
            );

            fcmService.sendMulticastNotification(fcmTokens, title, message, data);

            Notification notification = new Notification();
            notification.setMessage(message);
            notification.setReceiver(user);
            notification.setNotificationType(notificationType);
            notification.setTitle(title);
            notification.setSender(user);
            notificationRepository.save(notification);
        }
    }


    /**
     * @return
     */
    @Override
    public List<BudgetLastResponse> getLastBudget() {
        User user = UserUtils.getCurrentUser();
        List<BudgetLastResponse> responses = new ArrayList<>();

        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }

        List<Budget> budgets = budgetRepository.findByUserId(user.getUserId());
        if (budgets.isEmpty()) {
           return responses;
        }

        for (Budget budget : budgets) {
            List<BudgetPeriod> budgetPeriods = budgetPeriodRepository.findLatestByStatus(BudgetPeriodStatus.ACTIVE, budget, LocalDateTime.now());
            if (budgetPeriods.isEmpty()) {
                continue;
            }

            BudgetPeriod lastPeriod = budgetPeriods.get(budgetPeriods.size() - 1);

            List<BudgetCategoryLimit> budgetCategoryLimits = budgetCategoryLimitRepository.findByBudget(budget.getBudgetId());
            List<CategoryResponse> categoryResponses = new ArrayList<>();

            for (BudgetCategoryLimit budgetCategoryLimit : budgetCategoryLimits) {
                List<Category> result = categoryRepository.getCategoriesByCategoryId(
                        budgetCategoryLimit.getCategory().getCategoryId()
                );

                if (result != null) {
                    for (Category category : result) {
                        categoryResponses.add(CategoryResponse.builder()
                                .categoryId(category.getCategoryId())
                                .categoryName(category.getCategoryName())
                                .categoryIconUrl(category.getCategoryIconUrl())
                                .build());
                    }

                }

            }
            BudgetLastResponse response = BudgetLastResponse.builder()
                    .budgetId(budget.getBudgetId())
                    .budgetName(budget.getBudgetName())
                    .budgetRemainingAmount(lastPeriod.getRemainingAmount())
                    .overallAmountLimit(lastPeriod.getAmountLimit())
                    .startDate(lastPeriod.getStartDate())
                    .endDate(lastPeriod.getEndDate())
                    .periodType(budget.getPeriodType())
                    .status(lastPeriod.getStatus())
                    .categories(categoryResponses)
                    .build();

            responses.add(response);

        }

        return responses;

    }

    @Override
    public Page<BudgetResponse> getBudgetByName(String budgetName, Pageable pageable) {
        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (budgetName == null || budgetName.trim().isEmpty()) {
            throw new IllegalArgumentException("Budget name must not be null or empty");
        }
        
        Page<Budget> budgets = budgetRepository.findByUser_UserIdAndBudgetNameContaining(user.getUserId(), budgetName.trim(), pageable);
        return budgets.map(budgetMapper::toResponse);
    }

}