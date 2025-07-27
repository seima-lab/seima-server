package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.response.budgetPeriod.BudgetPeriodResponse;
import vn.fpt.seima.seimaserver.entity.Budget;
import vn.fpt.seima.seimaserver.entity.BudgetPeriod;
import vn.fpt.seima.seimaserver.entity.PeriodType;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.mapper.BudgetMapper;
import vn.fpt.seima.seimaserver.mapper.BudgetPeriodMapper;
import vn.fpt.seima.seimaserver.repository.BudgetPeriodRepository;
import vn.fpt.seima.seimaserver.repository.BudgetRepository;
import vn.fpt.seima.seimaserver.service.BudgetPeriodService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetPeriodServiceImpl implements BudgetPeriodService {
    private final BudgetPeriodRepository budgetPeriodRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetPeriodMapper budgetPeriodMapper;

    @Override
    public List<BudgetPeriod> generateBudgetPeriods(Budget budget) {
        List<BudgetPeriod> periods = new ArrayList<>();
        LocalDateTime start = budget.getStartDate();
        LocalDateTime end = budget.getEndDate();

        int index = 1;

        while (!start.isAfter(end)) {
            LocalDateTime periodEnd;
            switch (budget.getPeriodType()) {
                case WEEKLY:
                    periodEnd = start.plusDays(6);
                    break;
                case MONTHLY:
                    periodEnd = start.plusMonths(1).minusDays(1);
                    break;
                case YEARLY:
                    periodEnd = start.plusYears(1).minusDays(1);
                    break;
                default:
                    periodEnd = end;
            }

            if (periodEnd.isAfter(end)) {
                periodEnd = end;
            }

            BudgetPeriod period = new BudgetPeriod();
            period.setBudget(budget);
            period.setPeriodIndex(index++);
            period.setStartDate(start);
            period.setEndDate(periodEnd);
            period.setAmountLimit(budget.getOverallAmountLimit());
            period.setRemainingAmount(budget.getOverallAmountLimit());

            periods.add(period);

            start = periodEnd.plusDays(1);

            if (budget.getPeriodType() == PeriodType.NONE || budget.getPeriodType() == PeriodType.CUSTOM) {
                break;
            }
        }

        return periods;
    }

    @Override
    public Page<BudgetPeriodResponse> getListBudgetPeriods(Integer budgetId, Pageable pageable) {

        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found with id: " + budgetId));

        Page<BudgetPeriod> budgetPeriods = budgetPeriodRepository.getListBudgetPeriods(budget,pageable);

        return budgetPeriods.map(budgetPeriodMapper::toResponse);
    }
}
