package vn.fpt.seima.seimaserver.dto.response.transaction;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.fpt.seima.seimaserver.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionTodayResponse {
    private Integer transactionId;
    private Integer userId;
    private Integer walletId;
    private Integer categoryId;
    private Integer groupId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String currencyCode;
    private LocalDateTime transactionDate;
    private String description;
    private String receiptImageUrl;
    private String payeePayerName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    private BigDecimal balance;
}
