package vn.fpt.seima.seimaserver.dto.response.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDetailReportResponse {
    private BigDecimal totalExpense;
    private BigDecimal totalIncome;
    private Map<String, GroupDetail> data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GroupDetail {
        private BigDecimal expense = BigDecimal.ZERO;
        private BigDecimal income = BigDecimal.ZERO;
        private Integer categoryId;
        private String categoryName;
        private String categoryIconUrl;
    }
}
