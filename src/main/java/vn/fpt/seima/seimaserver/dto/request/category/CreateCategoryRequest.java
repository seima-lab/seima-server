package vn.fpt.seima.seimaserver.dto.request.category;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.fpt.seima.seimaserver.entity.Category;
import vn.fpt.seima.seimaserver.entity.CategoryType;
import vn.fpt.seima.seimaserver.entity.User;

import java.util.Set;
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateCategoryRequest {
    private Integer userId;
    private String categoryName;
    private CategoryType categoryType; // INCOME, EXPENSE
    private String categoryIconUrl;
    private Integer parentCategoryId;
    private Boolean isSystemDefined;
}
