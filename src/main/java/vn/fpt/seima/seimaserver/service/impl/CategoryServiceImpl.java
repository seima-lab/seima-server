package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.request.category.CreateCategoryRequest;
import vn.fpt.seima.seimaserver.dto.response.category.CategoryResponse;
import vn.fpt.seima.seimaserver.entity.Category;
import vn.fpt.seima.seimaserver.entity.CategoryType;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.mapper.CategoryMapper;
import vn.fpt.seima.seimaserver.repository.CategoryRepository;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.CategoryService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.util.List;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryMapper categoryMapper;
    private final UserRepository userRepository;
    private CategoryRepository categoryRepository;

    @Override
    public List<CategoryResponse> getAllCategoryByTypeAndUser(Integer categoryType, Integer userId) {
        CategoryType type = CategoryType.fromCode(categoryType);
        List<Category> categories = categoryRepository.findByCategoryTypeAndUser_UserIdOrUserIsNull(type, userId);
        return categories.stream().map(categoryMapper::toResponse).toList();
    }

    @Override
    public CategoryResponse getCategoryById(int id) {
        Category category = categoryRepository.findById(id).orElseThrow(
                ()->new ResourceNotFoundException("Category not found with id " + id)
        );
        return categoryMapper.toResponse(category);
    }

    @Override
    public CategoryResponse saveCategory(CreateCategoryRequest request) {
        if (request == null) {
            throw new ResourceNotFoundException("Category request is null");
        }
        if (categoryRepository.existsByCategoryName(request.getCategoryName())) {
            throw new IllegalArgumentException("Category name already exists");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Category category = categoryMapper.toEntity(request);
        category.setUser(user); // 👈 Gán User cho Category

        // (Optionally) xử lý parentCategory nếu bạn cần
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    public CategoryResponse updateCategory(Integer id, CreateCategoryRequest request) {
        Category existingCategory = categoryRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Category not found with id " + id));

        if(categoryRepository.existsByCategoryName(request.getCategoryName())&&
                !existingCategory.getCategoryName().equals(request.getCategoryName())) {
            throw new IllegalArgumentException("Category name already exists");
        }
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        categoryMapper.updateCategoryFromDto(request,existingCategory);
        existingCategory.setUser(user);

        Category savedCategory = categoryRepository.save(existingCategory);

        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    public void deleteCategory(int id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id " + id));

        // Không cho xóa nếu là hệ thống
        if (Boolean.TRUE.equals(category.getIsSystemDefined())) {
            throw new IllegalArgumentException("System-defined categories cannot be deleted.");
        }

        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("Cannot identify current user.");
        }
        if(category.getUser()== null ||!currentUser.getUserId().equals(category.getUser().getUserId())) {
            throw new IllegalArgumentException("You are not authorized to delete this category.");
        }
        categoryRepository.deleteById(id);
    }
}