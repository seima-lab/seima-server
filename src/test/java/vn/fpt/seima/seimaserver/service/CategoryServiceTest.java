package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import vn.fpt.seima.seimaserver.dto.request.category.CreateCategoryRequest;
import vn.fpt.seima.seimaserver.dto.response.category.CategoryResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.mapper.CategoryMapper;
import vn.fpt.seima.seimaserver.repository.CategoryRepository;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.repository.GroupRepository;
import vn.fpt.seima.seimaserver.service.impl.CategoryServiceImpl;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private User user;
    private Group group;
    private Category category;
    private CategoryResponse response;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1);
        group = new Group();
        group.setGroupId(1);
        category = new Category();
        category.setCategoryId(1);
        category.setCategoryName("Test Category");
        category.setUser(user);
        response = new CategoryResponse();
        response.setCategoryId(1);
    }

    @Test
    void testGetCategoryById_Success() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(response);
        CategoryResponse result = categoryService.getCategoryById(1);
        assertEquals(1, result.getCategoryId());
    }

    @Test
    void testGetCategoryById_NotFound() {
        when(categoryRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> categoryService.getCategoryById(1));
    }

    @Test
    void testSaveCategory_UserScope_Success() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setCategoryName("New Category");
        request.setCategoryType(CategoryType.INCOME);

        try (MockedStatic<UserUtils> userUtils = mockStatic(UserUtils.class)) {
            userUtils.when(UserUtils::getCurrentUser).thenReturn(user);
            when(categoryRepository.existsByCategoryName("New Category")).thenReturn(false);
            when(categoryRepository.existsByCategoryNameAndTypeAndUser_UserId(any(), any(), any())).thenReturn(false);
            when(categoryMapper.toEntity(request)).thenReturn(category);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(any(Category.class))).thenReturn(response);

            CategoryResponse result = categoryService.saveCategory(request);
            assertEquals(1, result.getCategoryId());
        }
    }

    @Test
    void testSaveCategory_GroupScope_Success() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setCategoryName("Group Category");
        request.setCategoryType(CategoryType.INCOME);
        request.setGroupId(1);

        try (MockedStatic<UserUtils> userUtils = mockStatic(UserUtils.class)) {
            userUtils.when(UserUtils::getCurrentUser).thenReturn(user);
            when(groupRepository.findById(1)).thenReturn(Optional.of(group));
            when(groupMemberRepository.existsByUserUserIdAndGroupGroupId(user.getUserId(), group.getGroupId())).thenReturn(true);
            when(categoryRepository.existsByCategoryNameAndTypeAndGroup_GroupId(any(), any(), any())).thenReturn(false);
            when(categoryMapper.toEntity(request)).thenReturn(category);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(any(Category.class))).thenReturn(response);

            CategoryResponse result = categoryService.saveCategory(request);
            assertEquals(1, result.getCategoryId());
        }
    }

    @Test
    void testUpdateCategory_Success() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setCategoryName("Updated Category");
        request.setCategoryType(CategoryType.INCOME);

        category.setIsSystemDefined(false);

        try (MockedStatic<UserUtils> userUtils = mockStatic(UserUtils.class)) {
            userUtils.when(UserUtils::getCurrentUser).thenReturn(user);
            when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
            when(categoryRepository.existsByCategoryNameAndTypeAndUser_UserId(any(), any(), any())).thenReturn(false);
            doNothing().when(categoryMapper).updateCategoryFromDto(request, category);
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(response);

            CategoryResponse result = categoryService.updateCategory(1, request);
            assertEquals(1, result.getCategoryId());
        }
    }

    @Test
    void testDeleteCategory_UserOwned_Success() {
        category.setIsSystemDefined(false);
        category.setUser(user);

        try (MockedStatic<UserUtils> userUtils = mockStatic(UserUtils.class)) {
            userUtils.when(UserUtils::getCurrentUser).thenReturn(user);
            when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
            doNothing().when(categoryRepository).deleteById(1);
            categoryService.deleteCategory(1);
            verify(categoryRepository).deleteById(1);
        }
    }

    @Test
    void testDeleteCategory_SystemDefined_Fail() {
        category.setIsSystemDefined(true);

        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        assertThrows(IllegalArgumentException.class, () -> categoryService.deleteCategory(1));
    }
}