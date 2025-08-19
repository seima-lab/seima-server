package vn.fpt.seima.seimaserver.dto.request.wallet;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateWalletRequest {
    @NotBlank(message = "Wallet name is required")
    private String walletName;

    @NotNull(message = "Balance is required")
    @Min(value = 0, message = "Balance cannot be negative")
    private BigDecimal balance;

    private String description;

    @NotNull(message = "Wallet type is required")
    private Integer walletTypeId;

    private Boolean isDefault = false;

    private Boolean excludeFromTotal = false;

    private Integer bankId;

    private String iconUrl;

    private String currencyCode;
    @NotNull(message = "Balance is required")
    @Min(value = 0, message = "Balance cannot be negative")
    private BigDecimal initialBalance;
} 