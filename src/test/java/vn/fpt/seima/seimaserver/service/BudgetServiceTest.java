package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.fpt.seima.seimaserver.dto.request.budget.CreateBudgetRequest;
import vn.fpt.seima.seimaserver.dto.response.budget.BudgetResponse;
import vn.fpt.seima.seimaserver.entity.Budget;
import vn.fpt.seima.seimaserver.entity.BudgetCategoryLimit;
import vn.fpt.seima.seimaserver.entity.Category;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.mapper.BudgetMapper;
import vn.fpt.seima.seimaserver.repository.BudgetCategoryLimitRepository;
import vn.fpt.seima.seimaserver.repository.BudgetRepository;
import vn.fpt.seima.seimaserver.service.impl.BudgetServiceImpl;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock private BudgetRepository budgetRepository;
    @Mock private BudgetCategoryLimitRepository budgetCategoryLimitRepository;
    @Mock private BudgetMapper budgetMapper;
    @InjectMocks private BudgetServiceImpl budgetService;

    private User user;
    private MockedStatic<UserUtils> mockedUserUtils;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1);

        // Đảm bảo đóng mock trước khi tạo mới
        if (mockedUserUtils != null) {
            mockedUserUtils.close();
        }
        mockedUserUtils = Mockito.mockStatic(UserUtils.class);
        mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        if (mockedUserUtils != null) {
            mockedUserUtils.close();
        }
    }

    @Test
    void getAllBudget_ShouldReturnPage() {
        Budget budget = new Budget();
        Page<Budget> page = new PageImpl<>(List.of(budget));
        when(budgetRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(budgetMapper.toResponse(budget)).thenReturn(new BudgetResponse());

        Page<BudgetResponse> result = budgetService.getAllBudget(Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getBudgetById_WhenExists() {
        Budget budget = new Budget();
        budget.setBudgetId(1);
        when(budgetRepository.findById(1)).thenReturn(Optional.of(budget));

        assertNotNull(budgetService.getBudgetById(1));
    }

    @Test
    void saveBudget_ShouldSaveAndReturnResponse() {
        // Arrange
        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setBudgetName("Test Budget");
        request.setOverallAmountLimit(BigDecimal.TEN);

        // Tạo một danh sách Category giả
        ArrayList<Category> categories = new ArrayList<>();
        categories.add(new Category());
        request.setCategoryList(categories);

        Budget budget = new Budget();

        // Mock dữ liệu
        lenient().when(budgetRepository.existsByBudgetName(anyString())).thenReturn(false);
        lenient().when(budgetMapper.toEntity(request)).thenReturn(budget);
        lenient().when(budgetRepository.save(budget)).thenReturn(budget);
        lenient().when(budgetMapper.toResponse(budget)).thenReturn(new BudgetResponse());

        // Act
        BudgetResponse result = budgetService.saveBudget(request);

        // Assert
        assertNotNull(result);
    }

    @Test
    void updateBudget_ShouldUpdateAndReturnResponse() {
        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setBudgetName("Updated Budget");
        request.setOverallAmountLimit(BigDecimal.ONE);
        request.setCategoryList(new ArrayList<>(List.of(new Category())));

        Budget budget = new Budget();
        budget.setBudgetName("Old");
        budget.setBudgetId(1);

        when(budgetRepository.findById(1)).thenReturn(Optional.of(budget));
        when(budgetRepository.existsByBudgetName("Updated Budget")).thenReturn(false);
        when(budgetRepository.save(budget)).thenReturn(budget);
        when(budgetMapper.toResponse(budget)).thenReturn(new BudgetResponse());

        BudgetResponse result = budgetService.updateBudget(1, request);
        assertNotNull(result);
    }

    @Test
    void deleteBudget_ShouldDeleteSuccessfully() {
        Budget budget = new Budget();
        budget.setBudgetId(1);
        when(budgetRepository.findById(1)).thenReturn(Optional.of(budget));

        budgetService.deleteBudget(1);
        verify(budgetCategoryLimitRepository).deleteByBudget_BudgetId(1);
        verify(budgetRepository).deleteById(1);
    }

    @Test
    void reduceAmount_ShouldUpdateBudgets() {
        // Prepare Budget
        Budget budget = new Budget();
        budget.setStartDate(LocalDateTime.now().minusDays(1));
        budget.setEndDate(LocalDateTime.now().plusDays(1));
        budget.setBudgetRemainingAmount(BigDecimal.valueOf(100));
        budget.setCurrencyCode("VND");

        // Prepare BudgetCategoryLimit
        BudgetCategoryLimit budgetCategoryLimit = new BudgetCategoryLimit();
        budgetCategoryLimit.setBudget(budget); // Gán budget đúng vào limit

        // Mock repositories
        when(budgetRepository.findByUserId(1)).thenReturn(List.of(budget));
        when(budgetCategoryLimitRepository.findByTransaction(anyInt()))
                .thenReturn(List.of(budgetCategoryLimit));

        // Act
        budgetService.reduceAmount(1, 1, BigDecimal.TEN, LocalDateTime.now(), "update-subtract", "VND");

        // Assert
        verify(budgetRepository).saveAll(anyList());
        assertEquals(BigDecimal.valueOf(90), budget.getBudgetRemainingAmount());
    }
    @Test
    void testReduceAmount_expense_success() {
        // Arrange
        Integer userId = 1;
        Integer categoryId = 10;
        BigDecimal amount = new BigDecimal("100");
        String type = "EXPENSE";
        String currency = "VND";
        LocalDateTime now = LocalDateTime.now();

        Budget budget = new Budget();
        budget.setBudgetId(1);
        budget.setCurrencyCode(currency);
        budget.setStartDate(now.minusDays(1));
        budget.setEndDate(now.plusDays(1));
        budget.setBudgetRemainingAmount(new BigDecimal("1000"));

        BudgetCategoryLimit limit = new BudgetCategoryLimit();
        Category category = new Category();
        category.setCategoryId(categoryId);
        limit.setCategory(category);

        when(budgetRepository.findByUserId(userId)).thenReturn(List.of(budget));
        when(budgetCategoryLimitRepository.findByTransaction(categoryId)).thenReturn(List.of(limit));

        // Act
        budgetService.reduceAmount(userId, categoryId, amount, now, type, currency);

        // Assert
        assertEquals(new BigDecimal("900"), budget.getBudgetRemainingAmount());
        verify(budgetRepository, times(1)).saveAll(any());
    }

    @Test
    void testReduceAmount_budgetNotFound_shouldThrowException() {
        when(budgetRepository.findByUserId(1)).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            budgetService.reduceAmount(1, 10, BigDecimal.TEN, LocalDateTime.now(), "EXPENSE", "VND");
        });

        assertEquals("Budget not found", ex.getMessage().trim()); // dùng .trim() để tránh lỗi ký tự vô hình
    }

    @Test
    void testReduceAmount_budgetCategoryLimitNotFound_shouldThrowException() {
        // Arrange
        Integer userId = 1;
        Integer categoryId = 10;

        Budget budget = new Budget();
        budget.setCurrencyCode("VND");
        budget.setStartDate(LocalDateTime.now().minusDays(1));
        budget.setEndDate(LocalDateTime.now().plusDays(1));
        budget.setBudgetRemainingAmount(BigDecimal.valueOf(1000));

        when(budgetRepository.findByUserId(userId)).thenReturn(List.of(budget));
        when(budgetCategoryLimitRepository.findByTransaction(categoryId)).thenReturn(Collections.emptyList());

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            budgetService.reduceAmount(userId, categoryId, BigDecimal.TEN, LocalDateTime.now(), "EXPENSE", "VND");
        });

        assertEquals("Budget category limit not found", ex.getMessage());
    }
}