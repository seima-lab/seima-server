package vn.fpt.seima.seimaserver.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.entity.Budget;
import vn.fpt.seima.seimaserver.service.BudgetService;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/budgets")
public class BudgetController {
    private BudgetService budgetService;

    @GetMapping()
    public ResponseEntity<ApiResponse<Page<Budget>>> getAllBudgets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Budget> budgets = budgetService.getAllBudget(pageable);

        ApiResponse<Page<Budget>> response = new ApiResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Lấy danh sách ngân sách thành công");
        response.setData(budgets);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Budget>> getBudgetById(@PathVariable int id) {

        try {
            Budget budget = budgetService.getBudgetById(id);

            ApiResponse<Budget> response = new ApiResponse<>();
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Lấy danh sách ngân sách thành công");
            response.setData(budget);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @PostMapping("")
    public ResponseEntity<ApiResponse<Budget>> createBudget(@RequestBody Budget budget) {

        try {
            Budget budgetCreate = budgetService.saveBudget(budget);

            ApiResponse<Budget> response = new ApiResponse<>();
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Lấy danh sách ngân sách thành công");
            response.setData(budgetCreate);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<Budget>> createBudget(@RequestBody Budget budget, @PathVariable int id) {

        try {
            Budget budgetCreate = budgetService.saveBudget(budget);

            ApiResponse<Budget> response = new ApiResponse<>();
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Lấy danh sách ngân sách thành công");
            response.setData(budgetCreate);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
} 