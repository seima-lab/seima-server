package vn.fpt.seima.seimaserver.dto.response.transaction;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionOcrResponse {
    @JsonProperty("total_amount")
    private BigDecimal amount;

    @JsonProperty("currency_code")
    private String currencyCode;

    @JsonProperty("transaction_date")
    private String  transactionDate;

    @JsonProperty("description_invoice")
    private String description;

    private String receiptImageUrl;

    @JsonProperty("customer_name")
    private String payeePayerName;


}
