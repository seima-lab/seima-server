package vn.fpt.seima.seimaserver.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.fpt.seima.seimaserver.service.BudgetCategoryLimitService;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/budget-category-limits")
public class BudgetCategoryLimitController {
    private BudgetCategoryLimitService budgetCategoryLimitService;
} 