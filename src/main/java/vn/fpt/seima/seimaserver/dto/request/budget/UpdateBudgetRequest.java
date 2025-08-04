package vn.fpt.seima.seimaserver.dto.request.budget;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.fpt.seima.seimaserver.entity.Category;
import vn.fpt.seima.seimaserver.entity.PeriodType;
import vn.fpt.seima.seimaserver.entity.Wallet;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class UpdateBudgetRequest {
    private Integer userId;
    private String budgetName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;
    private PeriodType periodType;
    private BigDecimal overallAmountLimit;
    private String currencyCode;
    private BigDecimal budgetRemainingAmount;
    private List<Wallet> walletList;
    private List<Category> categoryList;
    private boolean isUpdateAmount;
}
