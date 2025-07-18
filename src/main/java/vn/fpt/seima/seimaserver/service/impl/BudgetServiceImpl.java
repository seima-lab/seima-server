package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fpt.seima.seimaserver.dto.request.budget.CreateBudgetRequest;
import vn.fpt.seima.seimaserver.dto.response.budget.BudgetResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.mapper.BudgetMapper;
import vn.fpt.seima.seimaserver.repository.BudgetCategoryLimitRepository;
import vn.fpt.seima.seimaserver.repository.BudgetPeriodRepository;
import vn.fpt.seima.seimaserver.repository.BudgetRepository;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.BudgetCategoryLimitService;
import vn.fpt.seima.seimaserver.service.BudgetPeriodService;
import vn.fpt.seima.seimaserver.service.BudgetService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j

public class BudgetServiceImpl implements BudgetService {
    private  BudgetRepository budgetRepository;
    private final BudgetMapper budgetMapper;
    private final BudgetCategoryLimitRepository budgetCategoryLimitRepository;
    private final BudgetPeriodRepository budgetPeriodRepository;
    private final BudgetPeriodService budgetPeriodService;

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
        Budget budget = budgetMapper.toEntity(request);
        budget.setUser(user);
        budget.setPeriodType(PeriodType.NONE);
        Budget savedBudget = budgetRepository.save(budget);

        for (Category category : request.getCategoryList()) {
            BudgetCategoryLimit budgetCategoryLimit = new BudgetCategoryLimit();
            budgetCategoryLimit.setCategory(category);
            budgetCategoryLimit.setBudget(budget);

            budgetCategoryLimitRepository.save(budgetCategoryLimit);
        }
        return budgetMapper.INSTANCE.toResponse(savedBudget);
    }

    @Override
    @Transactional
    public BudgetResponse updateBudget(Integer id,CreateBudgetRequest request) {
        Budget existingBudget = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found for this id: " + id));

        if (budgetRepository.existsByBudgetName(request.getBudgetName()) &&
                !existingBudget.getBudgetName().equals(request.getBudgetName())) {
            throw new IllegalArgumentException("Budget name already exists");
        }

        if (request.getOverallAmountLimit().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Overall amount limit must be greater than zero");
        }

        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (request.getCategoryList().isEmpty()) {
            throw new IllegalArgumentException("Category list must not be empty");
        }
        budgetCategoryLimitRepository.deleteBudgetCategoryLimitByBudget(existingBudget.getBudgetId());
        budgetPeriodRepository.deleteAll(budgetPeriodRepository.findByBudget_BudgetId(existingBudget.getBudgetId()));

        budgetMapper.updateBudgetFromDto(request, existingBudget);
        existingBudget.setUser(user);

        Budget updatedBudget = budgetRepository.save(existingBudget);
        for (Category category : request.getCategoryList()) {
            BudgetCategoryLimit budgetCategoryLimit = new BudgetCategoryLimit();
            budgetCategoryLimit.setCategory(category);
            budgetCategoryLimit.setBudget(existingBudget);

            budgetCategoryLimitRepository.save(budgetCategoryLimit);
        }
        List<BudgetPeriod> periods = budgetPeriodService.generateBudgetPeriods(updatedBudget);
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
        budgetRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void reduceAmount(Integer userId, Integer categoryId ,BigDecimal amount, LocalDateTime transactionDate, String type, String code) {
        List<Budget> existingBudget =  budgetRepository.findByUserId(userId);

        if (existingBudget.isEmpty()) {
          throw new IllegalArgumentException("Budget not found ");
        }

        for (Budget budget : existingBudget) {

            List<BudgetCategoryLimit> budgetCategoryLimits = budgetCategoryLimitRepository
                    .findByTransaction(categoryId);

            if (budgetCategoryLimits.isEmpty()) {
               return;
            }
            if(budget.getCurrencyCode().equals(code)){
                if (transactionDate.isBefore(budget.getEndDate()) && transactionDate.isAfter(budget.getStartDate())) {
                    if (type.equals("EXPENSE")) {
                        BigDecimal newAmount = budget.getBudgetRemainingAmount().subtract(amount);
                        log.info("123 :" + newAmount);
                        budget.setBudgetRemainingAmount(newAmount);
                    }
                    else if (type.equals("INCOME")) {
                        budget.setBudgetRemainingAmount(budget.getBudgetRemainingAmount());
                    }
                    else if (type.equals("update-subtract")){
                        BigDecimal newAmount = budget.getBudgetRemainingAmount().subtract(amount);
                        budget.setBudgetRemainingAmount(newAmount);
                    }
                    else if (type.equals("update-add")) {
                        BigDecimal newAmount = budget.getBudgetRemainingAmount().add(amount);
                        budget.setBudgetRemainingAmount(newAmount);
                    }
                    else{
                        budget.setBudgetRemainingAmount(budget.getBudgetRemainingAmount());
                    }
                }

            }
        }
        budgetRepository.saveAll(existingBudget);
    }
}