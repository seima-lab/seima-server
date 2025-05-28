package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.repository.BudgetCategoryLimitRepository;
import vn.fpt.seima.seimaserver.service.BudgetCategoryLimitService;

@Service
@AllArgsConstructor
public class BudgetCategoryLimitServiceImpl implements BudgetCategoryLimitService {
    private BudgetCategoryLimitRepository budgetCategoryLimitRepository;
} 