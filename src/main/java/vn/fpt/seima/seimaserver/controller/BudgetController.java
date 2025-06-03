package vn.fpt.seima.seimaserver.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.fpt.seima.seimaserver.service.BudgetService;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/budgets")
public class BudgetController {
    private BudgetService budgetService;

} 