package vn.fpt.seima.seimaserver.dto.response.budget;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class FinancialHealthResponse {
    private Integer score;
    private String level;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    private BigDecimal balance;
    private IncomeExpenseSummary incomeExpense;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IncomeExpenseSummary {
        private BigDecimal income;
        private BigDecimal expense;
    }
}
