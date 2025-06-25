package vn.fpt.seima.seimaserver.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.budget.CreateBudgetRequest;
import vn.fpt.seima.seimaserver.dto.response.budget.BudgetResponse;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.service.BudgetService;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/budgets")
public class BudgetController {
    private BudgetService budgetService;

    @GetMapping()
    public ApiResponse<Page<BudgetResponse>> getAllBudgets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<BudgetResponse> budgets = budgetService.getAllBudget(pageable);

            return new ApiResponse<>(HttpStatus.OK.value(), "Budget list retrieved successfully", budgets);
        } catch (Exception ex) {
            return new ApiResponse<>(500, "An unexpected error occurred", null);
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<BudgetResponse> getBudgetById(@PathVariable int id) {
        try {
            BudgetResponse budget = budgetService.getBudgetById(id);

            return new ApiResponse<>(HttpStatus.OK.value(), "Budget list retrieved successfully", budget);
        } catch (ResourceNotFoundException ex) {
            return new ApiResponse<>(404, ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(500, "An unexpected error occurred", null);
        }
    }

    @PostMapping()
    public ApiResponse<BudgetResponse> createBudget(@RequestBody CreateBudgetRequest request) {
        try {
            BudgetResponse budgetCreate = budgetService.saveBudget(request);

            return new ApiResponse<>(HttpStatus.OK.value(), "Budget created successfully", budgetCreate);
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(400, ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(500, "An unexpected error occurred", null);
        }
    }

    @PutMapping("/update/{id}")
    public ApiResponse<BudgetResponse> updateBudget(@RequestBody CreateBudgetRequest request, @PathVariable int id) {
        try {
            BudgetResponse budgetUpdate = budgetService.updateBudget(id, request);

            return new ApiResponse<>(HttpStatus.OK.value(), "Budget updated successfully", budgetUpdate);
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(400, ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(500, "An unexpected error occurred", null);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<BudgetResponse> deleteBudget(@PathVariable("id") int id) {
        try {
            budgetService.deleteBudget(id);

            return new ApiResponse<>(200, "Budget deleted successfully", null);
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(404, ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(500, "An unexpected error occurred", null);
        }
    }
} 