package vn.fpt.seima.seimaserver.dto.request.transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import vn.fpt.seima.seimaserver.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionRequest {
    private Integer userId;
    private Integer walletId;
    private Integer categoryId;
    private Integer groupId; // optional
    private BigDecimal amount;
    private String currencyCode;
    private LocalDateTime transactionDate;
    private String description;
    private String receiptImageUrl;
    private String payeePayerName;
}