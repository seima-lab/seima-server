package vn.fpt.seima.seimaserver.dto.response.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.fpt.seima.seimaserver.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        private Integer transactionId;
        private TransactionType transactionType;
        private BigDecimal amount;
        private String currencyCode;
        private LocalDateTime transactionDate;
        private String description;
    }
}
