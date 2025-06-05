package vn.fpt.seima.seimaserver.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.fpt.seima.seimaserver.dto.request.category.CreateCategoryRequest;
import vn.fpt.seima.seimaserver.dto.response.category.CategoryResponse;

public interface CategoryService {
    Page<CategoryResponse> getAllCategory(Pageable pageable);

    CategoryResponse getCategoryById(int id);

    CategoryResponse saveCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(Integer id,CreateCategoryRequest category);

    void deleteCategory(int id);
} 