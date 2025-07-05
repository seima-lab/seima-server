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
        Budget budget = new Budget();
        budget.setStartDate(LocalDateTime.now().minusDays(1));
        budget.setEndDate(LocalDateTime.now().plusDays(1));
        budget.setBudgetRemainingAmount(BigDecimal.valueOf(100));

        when(budgetRepository.findByUserId(1)).thenReturn(List.of(budget));
        when(budgetCategoryLimitRepository.findByTransaction(anyInt()))
                .thenReturn(List.of(new BudgetCategoryLimit()));

        budgetService.reduceAmount(1, 1, BigDecimal.TEN, LocalDateTime.now(), "update", "VND");
        verify(budgetRepository).saveAll(anyList());
    }
}