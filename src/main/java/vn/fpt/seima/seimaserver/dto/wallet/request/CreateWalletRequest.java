package vn.fpt.seima.seimaserver.dto.wallet.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWalletRequest {
    private String walletName;
    private BigDecimal balance;
    private String description;
    private Long walletTypeId;  // ID của loại ví/ngân hàng
    private Boolean isDefault;  // Đặt làm ví mặc định
    private Boolean excludeFromTotal; // Loại trừ khỏi tổng
    private Integer userId;
    private String bankName;  // Tên ngân hàng
    private String iconUrl;  // Logo của ngân hàng
} 