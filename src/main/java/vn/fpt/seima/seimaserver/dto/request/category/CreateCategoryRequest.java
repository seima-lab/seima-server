package vn.fpt.seima.seimaserver.dto.request.category;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.fpt.seima.seimaserver.entity.CategoryType;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateCategoryRequest {
    private Integer userId;
    private Integer groupId;
    private String categoryName;
    private CategoryType categoryType; // INCOME, EXPENSE
    private String categoryIconUrl;
    private Integer parentCategoryId;
    private Boolean isSystemDefined;
}
