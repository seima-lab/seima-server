package vn.fpt.seima.seimaserver.dto.response.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionCategoryReportResponse {
    private BigDecimal totalExpense;
    private BigDecimal averageExpense;
    private BigDecimal totalIncome;
    private BigDecimal averageIncome;
    private Map<String, GroupAmount> data;
    private Integer categoryId;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GroupAmount {
        private BigDecimal expense = BigDecimal.ZERO;
        private BigDecimal income = BigDecimal.ZERO;
    }
}
