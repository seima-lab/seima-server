package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.request.category.CreateCategoryRequest;
import vn.fpt.seima.seimaserver.dto.response.category.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAllCategoryByTypeAndUser(Integer categoryType, Integer userId, Integer groupId);

    CategoryResponse getCategoryById(int id);

    CategoryResponse saveCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(Integer id,CreateCategoryRequest category);

    void deleteCategory(int id);
}