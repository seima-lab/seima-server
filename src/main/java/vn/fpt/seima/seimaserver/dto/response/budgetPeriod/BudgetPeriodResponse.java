package vn.fpt.seima.seimaserver.dto.response.budgetPeriod;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BudgetPeriodResponse {
    private Integer periodIndex;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long amountLimit;
    private Long remainingAmount;
}
