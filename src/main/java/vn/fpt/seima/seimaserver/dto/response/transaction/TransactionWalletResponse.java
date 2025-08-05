package vn.fpt.seima.seimaserver.dto.response.transaction;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionWalletResponse {
    private Summary summary;
    private Map<String, List<ReportByWallet>> reportByWallet;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal currentBalance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportByWallet {
        private Integer categoryId;
        private String categoryName;
        private String categoryIconUrl;
        private BigDecimal amount;
        private BigDecimal balance;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime transactionDate;
    }
}
