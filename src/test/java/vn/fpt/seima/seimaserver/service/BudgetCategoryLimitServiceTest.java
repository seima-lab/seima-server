package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.fpt.seima.seimaserver.dto.request.budgetCategory.CreateBudgetCategoryLimitRequest;
import vn.fpt.seima.seimaserver.dto.response.budgetCategoryLimit.BudgetCategoryLimitResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.mapper.BudgetCategoryLimitMapper;
import vn.fpt.seima.seimaserver.repository.*;
import vn.fpt.seima.seimaserver.service.impl.BudgetCategoryLimitServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetCategoryLimitServiceTest {

    @Mock
    private BudgetCategoryLimitRepository budgetCategoryLimitRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BudgetCategoryLimitMapper budgetCategoryLimitMapper;

    @InjectMocks
    private BudgetCategoryLimitServiceImpl budgetCategoryLimitService;

    private CreateBudgetCategoryLimitRequest request;
    private Budget budget;
    private Category category;
    private BudgetCategoryLimit entity;
    private BudgetCategoryLimit savedEntity;
    private BudgetCategoryLimitResponse response;

    @BeforeEach
    void setUp() {
        request = new CreateBudgetCategoryLimitRequest();
        request.setBudgetId(1);
        request.setCategoryId(2);
        request.setAmountLimit(BigDecimal.valueOf(1000));

        budget = new Budget();
        category = new Category();
        entity = new BudgetCategoryLimit();
        savedEntity = new BudgetCategoryLimit();
        response = new BudgetCategoryLimitResponse();
    }

    @Test
    void getAllBudgetCategoryLimit_WhenSuccess_ReturnsPage() {
        // Given
        BudgetCategoryLimit limit = new BudgetCategoryLimit();
        Page<BudgetCategoryLimit> page = new PageImpl<>(List.of(limit));
        when(budgetCategoryLimitRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(budgetCategoryLimitMapper.toResponse(limit)).thenReturn(response);

        // When
        Page<BudgetCategoryLimitResponse> result = budgetCategoryLimitService.getAllBudgetCategoryLimit(Pageable.unpaged());

        // Then
        assertEquals(1, result.getTotalElements());
        verify(budgetCategoryLimitRepository).findAll(any(Pageable.class));
    }

    @Test
    void getBudgetCategoryLimitById_WhenFound_ReturnsResponse() {
        // Given
        when(budgetCategoryLimitRepository.findById(1)).thenReturn(Optional.of(entity));
        when(budgetCategoryLimitMapper.toResponse(entity)).thenReturn(response);

        // When
        BudgetCategoryLimitResponse result = budgetCategoryLimitService.getBudgetCategoryLimitById(1);

        // Then
        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    void getBudgetCategoryLimitById_WhenNotFound_ThrowsException() {
        // Given
        when(budgetCategoryLimitRepository.findById(1)).thenReturn(Optional.empty());

        // Then
        assertThrows(ResourceNotFoundException.class, () -> budgetCategoryLimitService.getBudgetCategoryLimitById(1));
    }

    @Test
    void saveBudgetCategoryLimit_WhenSuccess_ReturnsResponse() {
        // Given
        when(categoryRepository.findById(2)).thenReturn(Optional.of(category));
        when(budgetRepository.findById(1)).thenReturn(Optional.of(budget));
        when(budgetCategoryLimitMapper.toEntity(request)).thenReturn(entity);
        when(budgetCategoryLimitRepository.save(entity)).thenReturn(savedEntity);
        when(budgetCategoryLimitMapper.toResponse(savedEntity)).thenReturn(response);

        // When
        BudgetCategoryLimitResponse result = budgetCategoryLimitService.saveBudgetCategoryLimit(request);

        // Then
        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    void saveBudgetCategoryLimit_WhenCategoryNotFound_ThrowsException() {
        // Given
        when(categoryRepository.findById(2)).thenReturn(Optional.empty());

        // Then
        assertThrows(IllegalArgumentException.class, () -> budgetCategoryLimitService.saveBudgetCategoryLimit(request));
    }

    @Test
    void updateBudgetCategoryLimit_WhenSuccess_ReturnsResponse() {
        // Given
        when(budgetCategoryLimitRepository.findById(1)).thenReturn(Optional.of(entity));
        when(categoryRepository.findById(2)).thenReturn(Optional.of(category));
        when(budgetRepository.findById(1)).thenReturn(Optional.of(budget));
        doNothing().when(budgetCategoryLimitMapper).updateBudgetFromDto(request, entity);
        when(budgetCategoryLimitRepository.save(entity)).thenReturn(savedEntity);
        when(budgetCategoryLimitMapper.toResponse(savedEntity)).thenReturn(response);

        // When
        BudgetCategoryLimitResponse result = budgetCategoryLimitService.updateBudgetCategoryLimit(1, request);

        // Then
        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    void deleteBudgetCategoryLimit_WhenSuccess_DeleteCalled() {
        // Given
        when(budgetCategoryLimitRepository.findById(1)).thenReturn(Optional.of(entity));
        doNothing().when(budgetCategoryLimitRepository).deleteById(1);

        // When
        budgetCategoryLimitService.deleteBudgetCategoryLimit(1);

        // Then
        verify(budgetCategoryLimitRepository).deleteById(1);
    }

    @Test
    void deleteBudgetCategoryLimit_WhenNotFound_ThrowsException() {
        // Given
        when(budgetCategoryLimitRepository.findById(1)).thenReturn(Optional.empty());

        // Then
        assertThrows(IllegalArgumentException.class, () -> budgetCategoryLimitService.deleteBudgetCategoryLimit(1));
    }
}
