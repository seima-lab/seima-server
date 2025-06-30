package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fpt.seima.seimaserver.dto.request.budget.CreateBudgetRequest;
import vn.fpt.seima.seimaserver.dto.response.budget.BudgetResponse;
import vn.fpt.seima.seimaserver.entity.Budget;
import vn.fpt.seima.seimaserver.entity.BudgetCategoryLimit;
import vn.fpt.seima.seimaserver.entity.Category;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.mapper.BudgetMapper;
import vn.fpt.seima.seimaserver.repository.BudgetCategoryLimitRepository;
import vn.fpt.seima.seimaserver.repository.BudgetRepository;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.BudgetCategoryLimitService;
import vn.fpt.seima.seimaserver.service.BudgetService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class BudgetServiceImpl implements BudgetService {
    private  BudgetRepository budgetRepository;
    private final BudgetMapper budgetMapper;
    private final BudgetCategoryLimitRepository budgetCategoryLimitRepository;

    @Override
    public Page<BudgetResponse> getAllBudget(Pageable pageable) {
        Page<Budget> budgets = budgetRepository.findAll(pageable);

        return budgets.map(budgetMapper::toResponse);
    }

    @Override
    public BudgetResponse getBudgetById(int id) {
        Budget budget = budgetRepository.findById(id).orElseThrow(()
                -> new ResourceNotFoundException("Budget not found for this id :: " + id));
        return BudgetMapper.INSTANCE.toResponse(budget);
    }

    @Override
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

        Budget budget = budgetMapper.toEntity(request);
        budget.setUser(user);
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

        if(request.getOverallAmountLimit().compareTo(BigDecimal.ZERO) < 0){
            throw new IllegalArgumentException("Overall amount limit must be greater than zero");
        }

        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (request.getCategoryList().isEmpty()) {
            throw new IllegalArgumentException("Category list must not be empty");
        }
        budgetCategoryLimitRepository.deleteByBudget_BudgetId(existingBudget.getBudgetId());

        budgetMapper.updateBudgetFromDto(request, existingBudget);
        existingBudget.setUser(user);

        Budget updatedBudget = budgetRepository.save(existingBudget);
        for (Category category : request.getCategoryList()) {
            BudgetCategoryLimit budgetCategoryLimit = new BudgetCategoryLimit();
            budgetCategoryLimit.setCategory(category);
            budgetCategoryLimit.setBudget(existingBudget);

            budgetCategoryLimitRepository.save(budgetCategoryLimit);
        }
        return budgetMapper.toResponse(updatedBudget);

    }

    @Override
    @Transactional
    public void deleteBudget(int id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found for this id: " + id));

        budgetCategoryLimitRepository.deleteByBudget_BudgetId(budget.getBudgetId());
        budgetRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void reduceAmount(Integer userId, Integer categoryId ,BigDecimal amount, LocalDateTime transactionDate) {
        List<Budget> existingBudget =  budgetRepository.findByUserId(userId);

        if (existingBudget == null) {
          throw new IllegalArgumentException("Budget not found ");
        }

        for (Budget budget : existingBudget) {

            List<BudgetCategoryLimit> budgetCategoryLimits = budgetCategoryLimitRepository
                    .findByTransaction(categoryId);

            if (budgetCategoryLimits.isEmpty()) {
                throw new IllegalArgumentException("Budget category limit not found");
            }
            if (transactionDate.isBefore(budget.getEndDate()) && transactionDate.isAfter(budget.getStartDate())) {
                BigDecimal newAmount = budget.getBudgetRemainingAmount().subtract(amount);
                budget.setBudgetRemainingAmount(newAmount);
            }

        }
        budgetRepository.saveAll(existingBudget);
    }
}