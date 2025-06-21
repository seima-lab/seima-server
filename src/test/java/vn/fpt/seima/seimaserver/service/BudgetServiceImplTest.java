package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
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

class BudgetServiceImplTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BudgetMapper budgetMapper;

    @InjectMocks
    private BudgetServiceImpl budgetService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllBudget() {
        Budget budget = new Budget();
        Page<Budget> budgets = new PageImpl<>(List.of(budget));
        BudgetResponse budgetResponse = new BudgetResponse();

        when(budgetRepository.findAll(any(Pageable.class))).thenReturn(budgets);
        when(budgetMapper.toResponse(budget)).thenReturn(budgetResponse);

        Page<BudgetResponse> result = budgetService.getAllBudget(Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        verify(budgetRepository).findAll(any(Pageable.class));
    }

    @Test
    void testGetBudgetById_Found() {
        Budget budget = new Budget();
        budget.setBudgetId(1);
        BudgetResponse response = new BudgetResponse();

        when(budgetRepository.findById(1)).thenReturn(Optional.of(budget));
        when(budgetMapper.toResponse(budget)).thenReturn(response);

        BudgetResponse result = budgetService.getBudgetById(1);
        assertEquals(response, result);
    }

    @Test
    void testGetBudgetById_NotFound() {
        when(budgetRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> budgetService.getBudgetById(1));
    }

    @Test
    void testSaveBudget_Success() {
        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setBudgetName("New Budget");
        request.setOverallAmountLimit(BigDecimal.valueOf(1000));

        Budget budget = new Budget();
        Budget savedBudget = new Budget();
        BudgetResponse response = new BudgetResponse();

        User user = new User(); // Giả lập user đang login

        // Mock static UserUtils
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(user);
            when(budgetRepository.existsByBudgetName("New Budget")).thenReturn(false);
            when(budgetMapper.toEntity(request)).thenReturn(budget);
            when(budgetRepository.save(budget)).thenReturn(savedBudget);
            when(budgetMapper.toResponse(savedBudget)).thenReturn(response);

            BudgetResponse result = budgetService.saveBudget(request);
            assertEquals(response, result);
        }
    }

    @Test
    void testSaveBudget_BudgetNameExists() {
        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setBudgetName("Existing Budget");
        request.setOverallAmountLimit(BigDecimal.valueOf(1000));

        when(budgetRepository.existsByBudgetName("Existing Budget")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> budgetService.saveBudget(request));
    }

    @Test
    void testUpdateBudget_Success() {
        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setBudgetName("Updated Budget");
        request.setOverallAmountLimit(BigDecimal.valueOf(2000));

        Budget existingBudget = new Budget();
        existingBudget.setBudgetId(1);
        existingBudget.setBudgetName("Old Name");

        Budget updatedBudget = new Budget();
        BudgetResponse response = new BudgetResponse();

        User user = new User();

        when(budgetRepository.findById(1)).thenReturn(Optional.of(existingBudget));
        when(budgetRepository.existsByBudgetName("Updated Budget")).thenReturn(false);

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(user);
            doNothing().when(budgetMapper).updateBudgetFromDto(request, existingBudget);
            when(budgetRepository.save(existingBudget)).thenReturn(updatedBudget);
            when(budgetMapper.toResponse(updatedBudget)).thenReturn(response);

            BudgetResponse result = budgetService.updateBudget(1, request);
            assertEquals(response, result);
        }
    }

    @Test
    void testDeleteBudget_Success() {
        Budget budget = new Budget();
        when(budgetRepository.findById(1)).thenReturn(Optional.of(budget));
        doNothing().when(budgetRepository).deleteById(1);
        budgetService.deleteBudget(1);
        verify(budgetRepository).deleteById(1);
    }

    @Test
    void testDeleteBudget_NotFound() {
        when(budgetRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> budgetService.deleteBudget(1));
    }
}
