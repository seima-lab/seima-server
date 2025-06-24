package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
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
    void getCategoryById_WhenFound_ReturnsResponse() {
        // Given
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(response);

        // When
        CategoryResponse result = categoryService.getCategoryById(1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getCategoryId());
    }

    @Test
    void getCategoryById_WhenNotFound_ThrowsException() {
        // Given
        when(categoryRepository.findById(1)).thenReturn(Optional.empty());

        // Then
        assertThrows(ResourceNotFoundException.class, () -> categoryService.getCategoryById(1));
    }

    @Test
    void saveCategory_WhenUserScope_Success() {
        // Given
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setCategoryName("New Category");
        request.setCategoryType(CategoryType.INCOME);

        try (MockedStatic<UserUtils> userUtils = mockStatic(UserUtils.class)) {
            userUtils.when(UserUtils::getCurrentUser).thenReturn(user);
            when(categoryRepository.existsByCategoryNameAndTypeAndUser_UserId(any(), any(), any())).thenReturn(false);
            when(categoryMapper.toEntity(request)).thenReturn(category);
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(response);

            // When
            CategoryResponse result = categoryService.saveCategory(request);

            // Then
            assertEquals(1, result.getCategoryId());
        }
    }

    @Test
    void saveCategory_WhenGroupScope_Success() {
        // Given
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
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(response);

            // When
            CategoryResponse result = categoryService.saveCategory(request);

            // Then
            assertEquals(1, result.getCategoryId());
        }
    }

    @Test
    void updateCategory_WhenSuccess_ReturnsResponse() {
        // Given
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

            // When
            CategoryResponse result = categoryService.updateCategory(1, request);

            // Then
            assertEquals(1, result.getCategoryId());
        }
    }

    @Test
    void deleteCategory_WhenUserOwned_Success() {
        // Given
        category.setIsSystemDefined(false);
        category.setUser(user);

        try (MockedStatic<UserUtils> userUtils = mockStatic(UserUtils.class)) {
            userUtils.when(UserUtils::getCurrentUser).thenReturn(user);
            when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
            doNothing().when(categoryRepository).deleteById(1);

            // When
            categoryService.deleteCategory(1);

            // Then
            verify(categoryRepository).deleteById(1);
        }
    }

    @Test
    void deleteCategory_WhenSystemDefined_ThrowsException() {
        // Given
        category.setIsSystemDefined(true);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));

        // Then
        assertThrows(IllegalArgumentException.class, () -> categoryService.deleteCategory(1));
    }
}
