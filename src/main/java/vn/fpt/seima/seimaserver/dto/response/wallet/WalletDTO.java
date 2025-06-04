package vn.fpt.seima.seimaserver.dto.response.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDTO {
    private Integer id;
    private String currencyCode;
    private BigDecimal currentBalance;
    private String iconUrl;
    private Instant walletCreatedAt;
    private Boolean walletIsArchived;
    private String walletName;
    private Integer userId;
    private Long walletTypeId;
    private String walletTypeName;
    private Boolean isDefault;
    private Boolean excludeFromTotal;
    private String description;
} 