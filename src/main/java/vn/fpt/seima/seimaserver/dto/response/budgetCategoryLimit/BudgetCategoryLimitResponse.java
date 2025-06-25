package vn.fpt.seima.seimaserver.dto.response.budgetCategoryLimit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BudgetCategoryLimitResponse {
    private Integer budgetCategoryLimitId;
}
