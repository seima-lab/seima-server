package vn.fpt.seima.seimaserver.dto.budgetCategoryLimit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateBudgetCategoryLimit {
    private Integer budgetId;
    private Integer categoryId;
    private BigDecimal overallAmountLimit;
}
