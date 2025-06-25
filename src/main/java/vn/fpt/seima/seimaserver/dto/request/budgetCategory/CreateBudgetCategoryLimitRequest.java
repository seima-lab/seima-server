package vn.fpt.seima.seimaserver.dto.request.budgetCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateBudgetCategoryLimitRequest {
    private Integer budgetId;
    private Integer categoryId;
}
