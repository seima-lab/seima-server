package vn.fpt.seima.seimaserver.dto.response.transaction;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionOverviewResponse {

    private Summary summary;
    private List<DailyTransactions> byDate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal balance; // totalIncome - totalExpense
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor

    public static class DailyTransactions {
        private LocalDate date;
        private List<TransactionItem> transactions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor

    public static class TransactionItem {
        private Integer transactionId;
        private String categoryName;
        private String categoryIconUrl;
        private BigDecimal amount;
        private String transactionType;
        private String description;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")

        private LocalDateTime transactionDate;
        private Integer walletId;
        private String receiptImageUrl;
    }
}
