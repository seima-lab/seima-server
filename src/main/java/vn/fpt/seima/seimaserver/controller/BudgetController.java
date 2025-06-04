package vn.fpt.seima.seimaserver.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.budget.BudgetResponse;
import vn.fpt.seima.seimaserver.dto.budget.CreateBudgetRequest;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.service.BudgetService;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/budgets")
public class BudgetController {
    private BudgetService budgetService;

    @GetMapping()
    public ResponseEntity<ApiResponse<Page<BudgetResponse>>> getAllBudgets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);

            Page<BudgetResponse> budgets = budgetService.getAllBudget(pageable);

            ApiResponse<Page<BudgetResponse>> response = new ApiResponse<>();
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Budget list retrieved successfully");
            response.setData(budgets);

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ApiResponse<>(500, "An unexpected error occurred", null));
        }

    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> getBudgetById(@PathVariable int id) {

        try {
            BudgetResponse budget = budgetService.getBudgetById(id);

            ApiResponse<BudgetResponse> response = new ApiResponse<>();
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Budget retrieved successfully");
            response.setData(budget);

            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(404, ex.getMessage(), null));

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ApiResponse<>(500, "An unexpected error occurred", null));
        }

    }

    @PostMapping()
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(@RequestBody CreateBudgetRequest request) {

        try {
            BudgetResponse budgetCreate = budgetService.saveBudget(request);

            ApiResponse<BudgetResponse> response = new ApiResponse<>();
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Budget created successfully");
            response.setData(budgetCreate);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(400, ex.getMessage(), null));

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ApiResponse<>(500, "An unexpected error occurred", null));
        }

    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(@RequestBody CreateBudgetRequest request, @PathVariable int id) {

        try {
            BudgetResponse budgetCreate = budgetService.updateBudget(id, request);

            ApiResponse<BudgetResponse> response = new ApiResponse<>();
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Budget updated successfully");
            response.setData(budgetCreate);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(400, ex.getMessage(), null));

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ApiResponse<>(500, "An unexpected error occurred", null));
        }

    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> deleteBudget(@PathVariable("id") int id) {
        try {
            budgetService.deleteBudget(id);
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ApiResponse<>(
                            200, "Budget deleted successfully", null
                    )
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ApiResponse<>(
                            404, ex.getMessage(), null
                    )
            );
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ApiResponse<>(500, "An unexpected error occurred", null));

        }
    }
} 