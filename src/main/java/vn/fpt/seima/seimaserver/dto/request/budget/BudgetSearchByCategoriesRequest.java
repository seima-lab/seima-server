package vn.fpt.seima.seimaserver.dto.request.budget;

import lombok.Data;

import java.util.List;

@Data
public class BudgetSearchByCategoriesRequest {
    private List<Integer> categoryIds;
}

