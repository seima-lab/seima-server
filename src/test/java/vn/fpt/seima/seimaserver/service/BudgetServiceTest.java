package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.fpt.seima.seimaserver.dto.request.budget.CreateBudgetRequest;
import vn.fpt.seima.seimaserver.dto.response.budget.BudgetResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.mapper.BudgetMapper;
import vn.fpt.seima.seimaserver.repository.BudgetCategoryLimitRepository;
import vn.fpt.seima.seimaserver.repository.BudgetPeriodRepository;
import vn.fpt.seima.seimaserver.repository.BudgetRepository;
import vn.fpt.seima.seimaserver.service.impl.BudgetServiceImpl;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock private BudgetRepository budgetRepository;
    @Mock private BudgetCategoryLimitRepository budgetCategoryLimitRepository;
    @Mock private BudgetMapper budgetMapper;
    @Mock private BudgetPeriodRepository budgetPeriodRepository;
    @InjectMocks private BudgetServiceImpl budgetService;

    private User user;
    private MockedStatic<UserUtils> mockedUserUtils;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1);
        mockedUserUtils = Mockito.mockStatic(UserUtils.class);
        mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        mockedUserUtils.close();
    }

    @Test
    void getAllBudget_ShouldReturnPage() {
        Budget budget = new Budget();
        Page<Budget> page = new PageImpl<>(List.of(budget));

        when(budgetRepository.findByUser_UserId(eq(user.getUserId()), any())).thenReturn(page);
        when(budgetMapper.toResponse(budget)).thenReturn(new BudgetResponse());

        Page<BudgetResponse> result = budgetService.getAllBudget(Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        verify(budgetRepository).findByUser_UserId(user.getUserId(), Pageable.unpaged());
    }

    @Test
    void getBudgetById_WhenExists_ShouldReturnBudget() {
        Budget budget = new Budget();
        budget.setBudgetId(1);

        when(budgetRepository.findById(1)).thenReturn(Optional.of(budget));

        BudgetResponse result = budgetService.getBudgetById(1);
        assertNotNull(result);
        assertEquals(1, result.getBudgetId());
    }

    @Test
    void deleteBudget_BudgetNotFound_ShouldThrowException() {
        when(budgetRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                budgetService.deleteBudget(999));
    }

    @Test
    void reduceAmount_ShouldReduceRemainingAmount() {
        Budget budget = new Budget();
        budget.setBudgetId(1);
        budget.setStartDate(LocalDateTime.now().minusDays(1));
        budget.setEndDate(LocalDateTime.now().plusDays(1));
        budget.setCurrencyCode("VND");
        budget.setBudgetRemainingAmount(BigDecimal.valueOf(100));

        BudgetCategoryLimit limit = new BudgetCategoryLimit();
        limit.setBudget(budget);

        when(budgetRepository.findByUserId(1)).thenReturn(List.of(budget));
        when(budgetCategoryLimitRepository.findByTransaction(1)).thenReturn(List.of(limit));

        budgetService.reduceAmount(1, 1, BigDecimal.TEN, LocalDateTime.now(), "update-subtract", "VND");

        assertEquals(BigDecimal.valueOf(90), budget.getBudgetRemainingAmount());
        verify(budgetRepository).saveAll(any());
    }

}