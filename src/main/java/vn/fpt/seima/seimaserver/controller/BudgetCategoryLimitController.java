package vn.fpt.seima.seimaserver.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.response.budgetCategoryLimit.BudgetCategoryLimitResponse;
import vn.fpt.seima.seimaserver.dto.request.budgetCategory.CreateBudgetCategoryLimitRequest;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.service.BudgetCategoryLimitService;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/budget-category-limits")
public class BudgetCategoryLimitController {
    private BudgetCategoryLimitService budgetCategoryLimitService;

    @GetMapping()
    public ApiResponse<Page<BudgetCategoryLimitResponse>> getAllBudgetCategoryLimits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<BudgetCategoryLimitResponse> budgets = budgetCategoryLimitService.getAllBudgetCategoryLimit(pageable);

            return new ApiResponse<>(HttpStatus.OK.value(), "Budget Category Limit list retrieved successfully", budgets);
        } catch (Exception ex) {
            return new ApiResponse<>(500, "An unexpected error occurred", null);
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<BudgetCategoryLimitResponse> getBudgetCategoryLimitById(@PathVariable int id) {
        try {
            BudgetCategoryLimitResponse budget = budgetCategoryLimitService.getBudgetCategoryLimitById(id);

            return new ApiResponse<>(HttpStatus.OK.value(), "Budget Category Limit retrieved successfully", budget);
        } catch (ResourceNotFoundException ex) {
            return new ApiResponse<>(404, ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(500, "An unexpected error occurred", null);
        }
    }

    @PostMapping()
    public ApiResponse<BudgetCategoryLimitResponse> createBudgetCategoryLimit(@RequestBody CreateBudgetCategoryLimitRequest request) {
        try {
            BudgetCategoryLimitResponse budgetCreate = budgetCategoryLimitService.saveBudgetCategoryLimit(request);

            return new ApiResponse<>(HttpStatus.OK.value(), "Budget Category Limit created successfully", budgetCreate);
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(400, ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(500, "An unexpected error occurred", null);
        }
    }

    @PutMapping("/update/{id}")
    public ApiResponse<BudgetCategoryLimitResponse> createBudgetCategoryLimit(@RequestBody CreateBudgetCategoryLimitRequest request, @PathVariable int id) {
        try {
            BudgetCategoryLimitResponse budgetUpdate = budgetCategoryLimitService.updateBudgetCategoryLimit(id, request);

            return new ApiResponse<>(HttpStatus.OK.value(), "Budget Category Limit updated successfully", budgetUpdate);
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(400, ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(500, "An unexpected error occurred", null);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<BudgetCategoryLimitResponse> deleteBudgetCategoryLimit(@PathVariable("id") int id) {
        try {
            budgetCategoryLimitService.deleteBudgetCategoryLimit(id);

            return new ApiResponse<>(200, "Budget Category Limit deleted successfully", null);
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(404, ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(500, "An unexpected error occurred", null);
        }
    }
} 