package vn.fpt.seima.seimaserver.dto.response.budgetPeriod;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import vn.fpt.seima.seimaserver.entity.BudgetPeriodStatus;

import java.time.LocalDateTime;

@Data
public class BudgetPeriodResponse {
    private String budgetName;
    private Integer periodIndex;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")

    private LocalDateTime startDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")

    private LocalDateTime endDate;
    private Long amountLimit;
    private Long remainingAmount;
    private BudgetPeriodStatus status;
}
