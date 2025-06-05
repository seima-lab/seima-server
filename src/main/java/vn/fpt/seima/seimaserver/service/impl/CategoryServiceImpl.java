package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.request.category.CreateCategoryRequest;
import vn.fpt.seima.seimaserver.dto.response.category.CategoryResponse;
import vn.fpt.seima.seimaserver.entity.Category;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.mapper.CategoryMapper;
import vn.fpt.seima.seimaserver.repository.CategoryRepository;
import vn.fpt.seima.seimaserver.service.CategoryService;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryMapper categoryMapper;
    private CategoryRepository categoryRepository;

    @Override
    public Page<CategoryResponse> getAllCategory(Pageable pageable) {
        Page<Category> categorys = categoryRepository.findAll(pageable);
        return categorys.map( categoryMapper::toResponse);
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

        Category category = categoryMapper.toEntity(request);
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

        categoryMapper.updateCategoryFromDto(request,existingCategory);
        Category savedCategory = categoryRepository.save(existingCategory);

        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    public void deleteCategory(int id) {
        categoryRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Category not found with id " + id)
        );
        categoryRepository.deleteById(id);
    }
}