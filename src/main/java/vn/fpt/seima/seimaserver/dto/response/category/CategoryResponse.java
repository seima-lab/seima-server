package vn.fpt.seima.seimaserver.dto.response.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.fpt.seima.seimaserver.entity.CategoryType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryResponse {
    private Integer categoryId;
    private Integer userId;
    private Integer groupId;
    private String categoryName;
    private CategoryType categoryType; // INCOME, EXPENSE
    private String categoryIconUrl;
    private Integer parentCategoryId;
    private Boolean isSystemDefined;
}
