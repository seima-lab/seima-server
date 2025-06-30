package vn.fpt.seima.seimaserver.service;

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
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.mapper.BudgetMapper;
import vn.fpt.seima.seimaserver.repository.BudgetRepository;
import vn.fpt.seima.seimaserver.service.impl.BudgetServiceImpl;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BudgetMapper budgetMapper;

    @InjectMocks
    private BudgetServiceImpl budgetService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserId(1);
    }

    @Test
    void getAllBudget_ReturnsPageOfBudgetResponses() {
        // Arrange
        Budget budget = new Budget();
        Page<Budget> budgets = new PageImpl<>(List.of(budget));
        BudgetResponse budgetResponse = new BudgetResponse();

        when(budgetRepository.findAll(any(Pageable.class))).thenReturn(budgets);
        when(budgetMapper.toResponse(budget)).thenReturn(budgetResponse);

        // Act
        Page<BudgetResponse> result = budgetService.getAllBudget(Pageable.unpaged());

        // Assert
        assertEquals(1, result.getTotalElements());
        verify(budgetRepository).findAll(any(Pageable.class));
    }

    @Test
    void getBudgetById_WhenFound_ReturnsBudgetResponse() {
        // Arrange
        Budget budget = new Budget();
        budget.setBudgetId(1);
        BudgetResponse response = new BudgetResponse();
        response.setBudgetId(1);

        when(budgetRepository.findById(1)).thenReturn(Optional.of(budget));
        when(budgetMapper.toResponse(budget)).thenReturn(response);

        // Act
        BudgetResponse result = budgetService.getBudgetById(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getBudgetId());
    }

    @Test
    void getBudgetById_WhenNotFound_ThrowsException() {
        // Arrange
        when(budgetRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> budgetService.getBudgetById(1));
    }


    @Test
    void saveBudget_WhenBudgetNameExists_ThrowsException() {
        // Arrange
        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setBudgetName("Existing Budget");

        when(budgetRepository.existsByBudgetName("Existing Budget")).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> budgetService.saveBudget(request));
    }


    @Test
    void deleteBudget_WhenNotFound_ThrowsException() {
        // Arrange
        when(budgetRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> budgetService.deleteBudget(1));
    }
}
