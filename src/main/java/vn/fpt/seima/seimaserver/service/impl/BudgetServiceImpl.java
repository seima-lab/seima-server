package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.repository.BudgetRepository;
import vn.fpt.seima.seimaserver.service.BudgetService;

@Service
@AllArgsConstructor
public class BudgetServiceImpl implements BudgetService {
    private BudgetRepository budgetRepository;
} 