package vn.fpt.seima.seimaserver.dto.response.wallet;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class WalletResponse {
    private Integer id;
    private String walletName;
    private BigDecimal currentBalance;
    private String walletTypeName;  // Tên ngân hàng/loại ví
    private Boolean isDefault;      // Ví mặc định
    private Boolean excludeFromTotal; // Loại trừ khỏi tổng
    private String bankName;       // Tên ngân hàng
    private String iconUrl;        // Logo của ngân hàng
    private String currencyCode;   // Mã tiền tệ
}
