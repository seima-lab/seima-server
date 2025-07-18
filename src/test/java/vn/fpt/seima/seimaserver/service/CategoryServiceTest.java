package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.fpt.seima.seimaserver.dto.request.category.CreateCategoryRequest;
import vn.fpt.seima.seimaserver.dto.response.category.CategoryResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.mapper.CategoryMapper;
import vn.fpt.seima.seimaserver.repository.*;
import vn.fpt.seima.seimaserver.service.impl.CategoryServiceImpl;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CategoryMapper categoryMapper;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private GroupRepository groupRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock private BudgetService budgetService;
    @Mock private WalletService walletService;
    @Mock private BudgetCategoryLimitRepository budgetCategoryLimitRepository;
    @InjectMocks
    private CategoryServiceImpl categoryService;

    private MockedStatic<UserUtils> userUtilsMockedStatic;

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

        userUtilsMockedStatic = Mockito.mockStatic(UserUtils.class);
        userUtilsMockedStatic.when(UserUtils::getCurrentUser).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        userUtilsMockedStatic.close();
    }

    @Test
    void getCategoryById_WhenExists_ReturnsCategoryResponse() {
        given(categoryRepository.findById(1)).willReturn(Optional.of(category));
        given(categoryMapper.toResponse(category)).willReturn(response);

        CategoryResponse result = categoryService.getCategoryById(1);

        assertNotNull(result);
        assertEquals(1, result.getCategoryId());
    }

    @Test
    void getCategoryById_WhenNotFound_ThrowsResourceNotFoundException() {
        given(categoryRepository.findById(1)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.getCategoryById(1));
    }

    @Test
    void saveCategory_WhenUserScope_Success() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setCategoryName("New Category");
        request.setCategoryType(CategoryType.EXPENSE);

        given(categoryRepository.existsByCategoryNameAndTypeAndUser_UserId(any(), any(), eq(1))).willReturn(false);
        given(categoryMapper.toEntity(request)).willReturn(category);
        given(categoryRepository.save(category)).willReturn(category);
        given(categoryMapper.toResponse(category)).willReturn(response);

        CategoryResponse result = categoryService.saveCategory(request);

        assertEquals(1, result.getCategoryId());
    }

    @Test
    void saveCategory_WhenGroupScope_Success() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setCategoryName("Group Category");
        request.setCategoryType(CategoryType.INCOME);
        request.setGroupId(1);

        given(groupRepository.findById(1)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsByUserUserIdAndGroupGroupId(1, 1)).willReturn(true);
        given(categoryRepository.existsByCategoryNameAndTypeAndGroup_GroupId(any(), any(), eq(1))).willReturn(false);
        given(categoryMapper.toEntity(request)).willReturn(category);
        given(categoryRepository.save(category)).willReturn(category);
        given(categoryMapper.toResponse(category)).willReturn(response);

        CategoryResponse result = categoryService.saveCategory(request);

        assertEquals(1, result.getCategoryId());
    }

    @Test
    void updateCategory_WhenValid_ReturnsUpdatedResponse() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setCategoryName("Updated Category");
        request.setCategoryType(CategoryType.EXPENSE);
        category.setIsSystemDefined(false);

        given(categoryRepository.findById(1)).willReturn(Optional.of(category));
        given(categoryRepository.existsByCategoryNameAndTypeAndUser_UserId(any(), any(), eq(1))).willReturn(false);
        willDoNothing().given(categoryMapper).updateCategoryFromDto(request, category);
        given(categoryRepository.save(category)).willReturn(category);
        given(categoryMapper.toResponse(category)).willReturn(response);

        CategoryResponse result = categoryService.updateCategory(1, request);

        assertEquals(1, result.getCategoryId());
    }

    @Test
    void deleteCategory_WhenUserOwnsCategory_ShouldDeleteSuccessfully() {
        // Arrange
        int categoryId = 1;
        category.setCategoryId(categoryId);
        category.setIsSystemDefined(false);
        category.setUser(user);  // user đã được set trong @BeforeEach

        Transaction transaction = new Transaction();
        transaction.setAmount(BigDecimal.valueOf(100_000));
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setCurrencyCode("VND");

        Wallet wallet = new Wallet();
        wallet.setId(42);
        transaction.setWallet(wallet);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(transactionRepository.findAllByCategory_CategoryId(categoryId)).thenReturn(List.of(transaction));

        // Act
        categoryService.deleteCategory(categoryId);

        // Assert
        verify(budgetService).reduceAmount(
                eq(user.getUserId()),
                eq(categoryId),
                eq(transaction.getAmount()),
                eq(transaction.getTransactionDate()),
                eq("update-add"),
                eq(transaction.getCurrencyCode())
        );

        verify(walletService).reduceAmount(
                eq(wallet.getId()),
                eq(transaction.getAmount()),
                eq("update-add"),
                eq(transaction.getCurrencyCode())
        );

        verify(transactionRepository).deleteByCategory_CategoryId(categoryId);
        verify(budgetCategoryLimitRepository).deleteByCategory_CategoryId(categoryId);
        verify(categoryRepository).deleteById(categoryId);
    }

    @Test
    void deleteCategory_WhenSystemCategory_ShouldThrowException() {
        // Arrange
        int categoryId = 1;
        category.setCategoryId(categoryId);
        category.setIsSystemDefined(true);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                categoryService.deleteCategory(categoryId)
        );

        assertEquals("System-defined categories cannot be deleted.", exception.getMessage());
    }

    @Test
    void deleteCategory_WhenUserNotOwner_ShouldThrowException() {
        // Arrange
        int categoryId = 1;
        User anotherUser = new User();
        anotherUser.setUserId(999);
        category.setCategoryId(categoryId);
        category.setIsSystemDefined(false);
        category.setUser(anotherUser); // owner != current user

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                categoryService.deleteCategory(categoryId)
        );

        assertEquals("You are not authorized to delete this category.", exception.getMessage());
    }

    @Test
    void deleteCategory_WhenSystemDefined_ThrowsException() {
        category.setIsSystemDefined(true);
        given(categoryRepository.findById(1)).willReturn(Optional.of(category));

        assertThrows(IllegalArgumentException.class, () -> categoryService.deleteCategory(1));
    }
}
