package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.response.budgetPeriod.BudgetPeriodResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.mapper.BudgetMapper;
import vn.fpt.seima.seimaserver.mapper.BudgetPeriodMapper;
import vn.fpt.seima.seimaserver.repository.BudgetPeriodRepository;
import vn.fpt.seima.seimaserver.repository.BudgetRepository;
import vn.fpt.seima.seimaserver.service.BudgetPeriodService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
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
            LocalDateTime expectedEnd;
            int expectedDays = 0;

            switch (budget.getPeriodType()) {
                case WEEKLY:
                    expectedEnd = start.plusDays(6);
                    expectedDays = 7;
                    break;
                case MONTHLY:
                    expectedEnd = start.plusMonths(1).minusDays(1);
                    expectedDays = Period.between(start.toLocalDate(), expectedEnd.toLocalDate()).getDays() + 1;
                    break;
                case YEARLY:
                    expectedEnd = start.plusYears(1).minusDays(1);
                    expectedDays = Period.between(start.toLocalDate(), expectedEnd.toLocalDate()).getDays() + 1;
                    break;
                default:
                    expectedEnd = end;
                    expectedDays = (int) ChronoUnit.DAYS.between(start.toLocalDate(), expectedEnd.toLocalDate()) + 1;
            }

            if (expectedEnd.isAfter(end)) {
                expectedEnd = end;
            }

            // Tính actual days của chu kỳ hiện tại
            int actualDays = (int) ChronoUnit.DAYS.between(start.toLocalDate(), expectedEnd.toLocalDate()) + 1;

            BudgetPeriod period = new BudgetPeriod();
            period.setBudget(budget);
            period.setPeriodIndex(index++);
            period.setStartDate(start);
            period.setEndDate(expectedEnd);
            period.setAmountLimit(budget.getOverallAmountLimit());
            period.setRemainingAmount(budget.getOverallAmountLimit());

            if (actualDays < expectedDays) {
                period.setStatus(BudgetPeriodStatus.INACTIVE);
            } else {
                period.setStatus(BudgetPeriodStatus.ACTIVE);
            }

            periods.add(period);
            start = expectedEnd.plusDays(1);
        }

        return periods;
    }

    @Override
    public Page<BudgetPeriodResponse> getListBudgetPeriods(Integer budgetId, Pageable pageable) {

        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found with id: " + budgetId));
        LocalDateTime todayDate = LocalDateTime.now();
        Page<BudgetPeriod> budgetPeriods = budgetPeriodRepository.getListBudgetPeriods(budget, BudgetPeriodStatus.ACTIVE,pageable, todayDate);

        return budgetPeriods.map(budgetPeriodMapper::toResponse);
    }
}
