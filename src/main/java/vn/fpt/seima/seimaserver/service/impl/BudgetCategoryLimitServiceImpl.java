package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.response.budgetCategoryLimit.BudgetCategoryLimitResponse;
import vn.fpt.seima.seimaserver.dto.request.budgetCategory.CreateBudgetCategoryLimitRequest;
import vn.fpt.seima.seimaserver.entity.Budget;
import vn.fpt.seima.seimaserver.entity.BudgetCategoryLimit;
import vn.fpt.seima.seimaserver.entity.Category;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.mapper.BudgetCategoryLimitMapper;
import vn.fpt.seima.seimaserver.repository.BudgetCategoryLimitRepository;
import vn.fpt.seima.seimaserver.repository.BudgetRepository;
import vn.fpt.seima.seimaserver.repository.CategoryRepository;
import vn.fpt.seima.seimaserver.service.BudgetCategoryLimitService;

import java.math.BigDecimal;

@Service
@AllArgsConstructor
public class BudgetCategoryLimitServiceImpl implements BudgetCategoryLimitService {

    private final BudgetCategoryLimitRepository budgetCategoryLimitRepository;
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetCategoryLimitMapper budgetCategoryLimitMapper;

    @Override
    public Page<BudgetCategoryLimitResponse> getAllBudgetCategoryLimit(Pageable pageable) {

        Page<BudgetCategoryLimit> budgetCategoryLimit = budgetCategoryLimitRepository.findAll(pageable);

        return budgetCategoryLimit.map(budgetCategoryLimitMapper::toResponse);
    }

    @Override
    public BudgetCategoryLimitResponse getBudgetCategoryLimitById(int id) {

        BudgetCategoryLimit budgetCategoryLimit = budgetCategoryLimitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget Category Limit not found for this id :: " + id));

        return budgetCategoryLimitMapper.toResponse(budgetCategoryLimit);
    }

    @Override
    public BudgetCategoryLimitResponse saveBudgetCategoryLimit(CreateBudgetCategoryLimitRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + request.getCategoryId()));

        Budget budget = budgetRepository.findById(request.getBudgetId())
                .orElseThrow(() -> new IllegalArgumentException("Budget not found with id: " + request.getBudgetId()));

        if(request.getAmountLimit().compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Overall amount limit must be greater than zero");
        }

        BudgetCategoryLimit budgetCategoryLimit = budgetCategoryLimitMapper.toEntity(request);
        budgetCategoryLimit.setCategory(category);
        budgetCategoryLimit.setBudget(budget);

        BudgetCategoryLimit savedBudget = budgetCategoryLimitRepository.save(budgetCategoryLimit);

        return budgetCategoryLimitMapper.toResponse(savedBudget);
    }

    @Override
    public BudgetCategoryLimitResponse updateBudgetCategoryLimit(Integer id, CreateBudgetCategoryLimitRequest request) {
        BudgetCategoryLimit existingBudget = budgetCategoryLimitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Budget Category Limit not found for this id: " + id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + request.getCategoryId()));

        Budget budget = budgetRepository.findById(request.getBudgetId())
                .orElseThrow(() -> new IllegalArgumentException("Budget not found with id: " + request.getBudgetId()));

        if(request.getAmountLimit().compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Overall amount limit must be greater than zero");
        }

        budgetCategoryLimitMapper.updateBudgetFromDto(request, existingBudget);
        existingBudget.setBudget(budget);
        existingBudget.setCategory(category);
        BudgetCategoryLimit updatedBudget = budgetCategoryLimitRepository.save(existingBudget);

        return budgetCategoryLimitMapper.toResponse(updatedBudget);
    }

    @Override
    public void deleteBudgetCategoryLimit(int id) {
        budgetCategoryLimitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found for this id: " + id));

        budgetCategoryLimitRepository.deleteById(id);
    }
}