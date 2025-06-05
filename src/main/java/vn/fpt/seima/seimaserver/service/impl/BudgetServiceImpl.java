package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.request.budget.CreateBudgetRequest;
import vn.fpt.seima.seimaserver.dto.response.budget.BudgetResponse;
import vn.fpt.seima.seimaserver.entity.Budget;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.mapper.BudgetMapper;
import vn.fpt.seima.seimaserver.repository.BudgetRepository;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.BudgetService;

@Service
@AllArgsConstructor
public class BudgetServiceImpl implements BudgetService {
    private  BudgetRepository budgetRepository;
    private final BudgetMapper budgetMapper;
    private final UserRepository userRepository;


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
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + request.getUserId()));

        Budget budget = budgetMapper.toEntity(request);
        budget.setUser(user);
        Budget savedBudget = budgetRepository.save(budget);
        return budgetMapper.INSTANCE.toResponse(savedBudget);
    }

    @Override
    public BudgetResponse updateBudget(Integer id,CreateBudgetRequest request) {
        Budget existingBudget = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found for this id: " + id));

        if (budgetRepository.existsByBudgetName(request.getBudgetName()) &&
                !existingBudget.getBudgetName().equals(request.getBudgetName())) {
            throw new IllegalArgumentException("Budget name already exists");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + request.getUserId()));

        budgetMapper.updateBudgetFromDto(request, existingBudget);
        existingBudget.setUser(user);
        Budget updatedBudget = budgetRepository.save(existingBudget);

        return budgetMapper.toResponse(updatedBudget);

    }

    @Override
    public void deleteBudget(int id) {
        budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found for this id: " + id));

        budgetRepository.deleteById(id);
    }
}