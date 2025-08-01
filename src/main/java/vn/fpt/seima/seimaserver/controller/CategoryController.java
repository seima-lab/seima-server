package vn.fpt.seima.seimaserver.controller;


import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.category.CreateCategoryRequest;
import vn.fpt.seima.seimaserver.dto.response.category.CategoryResponse;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.service.CategoryService;


import java.util.List;


@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private CategoryService categoryService;


    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAllCategoriesByTypeAndUser(
            @RequestParam Integer categoryType,
            @RequestParam Integer groupId
    ) {
        try {
            List<CategoryResponse> categories = categoryService.getAllCategoryByTypeAndUser(categoryType, groupId);
            return new ApiResponse<>(HttpStatus.OK.value(), "Category list retrieved successfully", categories);
        } catch (Exception ex) {
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),ex.getMessage(), null);
        }
    }




    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getCategoryById(@PathVariable int id) {
        try {
            CategoryResponse category = categoryService.getCategoryById(id);


            return new ApiResponse<>(HttpStatus.OK.value(), "Category list retrieved successfully", category);
        } catch (ResourceNotFoundException ex) {
            return new ApiResponse<>(404, ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(500, "An unexpected error occurred", null);
        }
    }


    @PostMapping()
    public ApiResponse<CategoryResponse> createCategory(@RequestBody CreateCategoryRequest request) {
        try {
            CategoryResponse categoryCreate = categoryService.saveCategory(request);


            return new ApiResponse<>(HttpStatus.OK.value(), "Category created successfully", categoryCreate);
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(400, ex.getMessage(), null);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ApiResponse<>(500, "An unexpected error occurred", null);
        }
    }


    @PutMapping("/update/{id}")
    public ApiResponse<CategoryResponse> createCategory(@RequestBody CreateCategoryRequest request, @PathVariable int id) {
        try {
            CategoryResponse categoryUpdate = categoryService.updateCategory(id, request);


            return new ApiResponse<>(HttpStatus.OK.value(), "Category updated successfully", categoryUpdate);
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(400, ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(500, "An unexpected error occurred", null);
        }
    }


    @DeleteMapping("/delete/{id}")
    public ApiResponse<CategoryResponse> deleteCategory(@PathVariable("id") int id) {
        try {
            categoryService.deleteCategory(id);


            return new ApiResponse<>(200, "Category deleted successfully", null);
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(404, ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(500, "An unexpected error occurred", null);
        }
    }
}
