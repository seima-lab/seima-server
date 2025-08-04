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
            if (request.getStartDate().isBefore(request.getEndDate())) {
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
        Budget existingBudget = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found for this id: " + id));

        if (request.getOverallAmountLimit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Overall amount limit must be greater than zero");
        }
        if (request.isUpdateAmount()) {
            LocalDateTime dateTime = LocalDateTime.now();
            List<BudgetPeriod> budgetPeriods = budgetPeriodRepository.getListBudgetPeriodsFuture(existingBudget, BudgetPeriodStatus.ACTIVE, dateTime);
            for (BudgetPeriod budgetPeriod : budgetPeriods) {
                budgetPeriod.setAmountLimit(request.getOverallAmountLimit());
            }
        }
        if (budgetRepository.existsByBudgetName(request.getBudgetName()) &&
                !existingBudget.getBudgetName().equals(request.getBudgetName())) {
            throw new IllegalArgumentException("Budget name already exists");
        }

        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (request.getCategoryList().isEmpty()) {
            throw new IllegalArgumentException("Category list must not be empty");
        }
        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("Start date must not be null");
        }
        if (request.getEndDate() != null) {
            if (request.getStartDate().isBefore(request.getEndDate())) {
                throw new IllegalArgumentException("Start date must be before end date");
            }
        }
        if (request.getEndDate() == null) {
            request.setEndDate(LocalDateTime.of(LocalDate.now().getYear(), 12, 31, 23, 59, 59));
        }
        budgetCategoryLimitRepository.deleteBudgetCategoryLimitByBudget(existingBudget.getBudgetId());
        budgetPeriodRepository.deleteAll(budgetPeriodRepository.findByBudget_BudgetId(existingBudget.getBudgetId()));

        budgetMapper.updateBudgetFromDto(request, existingBudget);
        existingBudget.setUser(user);

        Budget updatedBudget = budgetRepository.save(existingBudget);
        List<Integer> categoryIds = new ArrayList<>();

        for (Category category : request.getCategoryList()) {
            BudgetCategoryLimit budgetCategoryLimit = new BudgetCategoryLimit();
            budgetCategoryLimit.setCategory(category);
            budgetCategoryLimit.setBudget(existingBudget);
            categoryIds.add(category.getCategoryId());

            budgetCategoryLimitRepository.save(budgetCategoryLimit);
        }
        List<Transaction> transactions = transactionRepository.listExpensesByCategoryAndMonth(
                user.getUserId(),
                categoryIds,
                request.getStartDate(),
                request.getEndDate()
        );
        List<BudgetPeriod> periods = budgetPeriodService.generateBudgetPeriods(updatedBudget);
        for (Transaction transaction : transactions) {
            for (BudgetPeriod period : periods) {
                if (!transaction.getTransactionDate().isBefore(period.getStartDate()) &&
                        !transaction.getTransactionDate().isAfter(period.getEndDate())) {
                    period.setRemainingAmount(period.getRemainingAmount().subtract(transaction.getAmount()));
                }
            }
        }

        budgetPeriodRepository.saveAll(periods);

        return budgetMapper.toResponse(updatedBudget);

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
    public void reduceAmount(Integer userId, Integer categoryId, BigDecimal amount, LocalDateTime transactionDate, String type, String code) {
        List<Budget> existingBudget = budgetRepository.findByUserId(userId);
        User user = UserUtils.getCurrentUser();
        if (user == null) {
            return;
        }
        List<Integer> userIds = Collections.singletonList(user.getUserId());
        List<String> fcmTokens = userDeviceRepository.findFcmTokensByUserIds(userIds);
        if (existingBudget.isEmpty()) {
            return;
        }
        for (Budget budget : existingBudget) {
            List<BudgetCategoryLimit> budgetCategoryLimits = budgetCategoryLimitRepository
                    .findByTransaction(categoryId);
            if (budgetCategoryLimits.isEmpty()) {
                return;
            }
            List<BudgetPeriod> budgetPeriods = budgetPeriodRepository.findByBudget_BudgetId(budget.getBudgetId());
            for (BudgetPeriod budgetPeriod : budgetPeriods) {
                if (transactionDate.isBefore(budgetPeriod.getEndDate()) && transactionDate.isAfter(budgetPeriod.getStartDate())) {
                    if (type.equals("EXPENSE")) {
                        BigDecimal newAmount = budgetPeriod.getRemainingAmount().subtract(amount);
                        budgetPeriod.setRemainingAmount(newAmount);
                        if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
                            Map<String, String> data = Map.of(
                                    "type", "budget_notification",
                                    "senderUserId", userId.toString(),
                                    "senderName", user.getUserFullName()
                            );

                            fcmService.sendMulticastNotification(fcmTokens, TITLE, MESSAGE, data);

                            Notification notification = new Notification();
                            notification.setMessage(MESSAGE);
                            notification.setReceiver(user);
                            notification.setNotificationType(NotificationType.BUDGET_LIMIT_EXCEEDED);
                            notification.setTitle(TITLE);
                            notification.setSender(user);
                            notificationRepository.save(notification);
                        }
                    } else if (type.equals("INCOME")) {
                        budgetPeriod.setRemainingAmount(budgetPeriod.getRemainingAmount());
                    } else if (type.equals("update-subtract")) {
                        BigDecimal newAmount = budgetPeriod.getRemainingAmount().subtract(amount);
                        budgetPeriod.setRemainingAmount(newAmount);
                    } else if (type.equals("update-add")) {
                        BigDecimal newAmount = budgetPeriod.getRemainingAmount().add(amount);
                        budgetPeriod.setRemainingAmount(newAmount);
                    } else {
                        budgetPeriod.setRemainingAmount(budgetPeriod.getRemainingAmount());
                    }
                }

            }
            budgetPeriodRepository.saveAll(budgetPeriods);

        }

    }

    /**
     * @return
     */
    @Override
    public List<BudgetLastResponse> getLastBudget() {
        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }

        List<Budget> budgets = budgetRepository.findByUserId(user.getUserId());
        if (budgets.isEmpty()) {
            throw new IllegalArgumentException("Budget not found");
        }

        List<BudgetLastResponse> responses = new ArrayList<>();
        for (Budget budget : budgets) {
            LocalDateTime timeNow = LocalDateTime.now();
            List<BudgetPeriod> budgetPeriods = budgetPeriodRepository.findByBudget_BudgetIdAndTime(budget.getBudgetId(), timeNow);
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