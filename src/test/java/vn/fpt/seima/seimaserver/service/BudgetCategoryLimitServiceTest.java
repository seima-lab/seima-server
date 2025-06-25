package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.fpt.seima.seimaserver.dto.request.budgetCategory.CreateBudgetCategoryLimitRequest;
import vn.fpt.seima.seimaserver.dto.response.budgetCategoryLimit.BudgetCategoryLimitResponse;
import vn.fpt.seima.seimaserver.entity.Budget;
import vn.fpt.seima.seimaserver.entity.BudgetCategoryLimit;
import vn.fpt.seima.seimaserver.entity.Category;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.mapper.BudgetCategoryLimitMapper;
import vn.fpt.seima.seimaserver.repository.BudgetCategoryLimitRepository;
import vn.fpt.seima.seimaserver.repository.BudgetRepository;
import vn.fpt.seima.seimaserver.repository.CategoryRepository;
import vn.fpt.seima.seimaserver.service.impl.BudgetCategoryLimitServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllBudgetCategoryLimit() {
        BudgetCategoryLimit limit = new BudgetCategoryLimit();
        Page<BudgetCategoryLimit> page = new PageImpl<>(List.of(limit));
        BudgetCategoryLimitResponse response = new BudgetCategoryLimitResponse();

        when(budgetCategoryLimitRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(budgetCategoryLimitMapper.toResponse(limit)).thenReturn(response);

        Page<BudgetCategoryLimitResponse> result = budgetCategoryLimitService.getAllBudgetCategoryLimit(Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetBudgetCategoryLimitById_Found() {
        BudgetCategoryLimit limit = new BudgetCategoryLimit();
        BudgetCategoryLimitResponse response = new BudgetCategoryLimitResponse();

        when(budgetCategoryLimitRepository.findById(1)).thenReturn(Optional.of(limit));
        when(budgetCategoryLimitMapper.toResponse(limit)).thenReturn(response);

        BudgetCategoryLimitResponse result = budgetCategoryLimitService.getBudgetCategoryLimitById(1);
        assertEquals(response, result);
    }

    @Test
    void testGetBudgetCategoryLimitById_NotFound() {
        when(budgetCategoryLimitRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> budgetCategoryLimitService.getBudgetCategoryLimitById(1));
    }

    @Test
    void testSaveBudgetCategoryLimit_Success() {
        CreateBudgetCategoryLimitRequest request = new CreateBudgetCategoryLimitRequest();
        request.setBudgetId(1);
        request.setCategoryId(2);

        Budget budget = new Budget();
        Category category = new Category();
        BudgetCategoryLimit entity = new BudgetCategoryLimit();
        BudgetCategoryLimit savedEntity = new BudgetCategoryLimit();
        BudgetCategoryLimitResponse response = new BudgetCategoryLimitResponse();

        when(categoryRepository.findById(2)).thenReturn(Optional.of(category));
        when(budgetRepository.findById(1)).thenReturn(Optional.of(budget));
        when(budgetCategoryLimitMapper.toEntity(request)).thenReturn(entity);
        when(budgetCategoryLimitRepository.save(entity)).thenReturn(savedEntity);
        when(budgetCategoryLimitMapper.toResponse(savedEntity)).thenReturn(response);

        BudgetCategoryLimitResponse result = budgetCategoryLimitService.saveBudgetCategoryLimit(request);
        assertEquals(response, result);
    }

    @Test
    void testSaveBudgetCategoryLimit_CategoryNotFound() {
        CreateBudgetCategoryLimitRequest request = new CreateBudgetCategoryLimitRequest();
        request.setCategoryId(2);
        request.setBudgetId(1);

        when(categoryRepository.findById(2)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> budgetCategoryLimitService.saveBudgetCategoryLimit(request));
    }

    @Test
    void testUpdateBudgetCategoryLimit_Success() {
        CreateBudgetCategoryLimitRequest request = new CreateBudgetCategoryLimitRequest();
        request.setBudgetId(1);
        request.setCategoryId(2);

        BudgetCategoryLimit existingLimit = new BudgetCategoryLimit();
        Budget budget = new Budget();
        Category category = new Category();
        BudgetCategoryLimit updatedLimit = new BudgetCategoryLimit();
        BudgetCategoryLimitResponse response = new BudgetCategoryLimitResponse();

        when(budgetCategoryLimitRepository.findById(1)).thenReturn(Optional.of(existingLimit));
        when(categoryRepository.findById(2)).thenReturn(Optional.of(category));
        when(budgetRepository.findById(1)).thenReturn(Optional.of(budget));
        doNothing().when(budgetCategoryLimitMapper).updateBudgetFromDto(request, existingLimit);
        when(budgetCategoryLimitRepository.save(existingLimit)).thenReturn(updatedLimit);
        when(budgetCategoryLimitMapper.toResponse(updatedLimit)).thenReturn(response);

        BudgetCategoryLimitResponse result = budgetCategoryLimitService.updateBudgetCategoryLimit(1, request);
        assertEquals(response, result);
    }

    @Test
    void testDeleteBudgetCategoryLimit_Success() {
        BudgetCategoryLimit limit = new BudgetCategoryLimit();

        when(budgetCategoryLimitRepository.findById(1)).thenReturn(Optional.of(limit));
        doNothing().when(budgetCategoryLimitRepository).deleteById(1);

        budgetCategoryLimitService.deleteBudgetCategoryLimit(1);
        verify(budgetCategoryLimitRepository).deleteById(1);
    }

    @Test
    void testDeleteBudgetCategoryLimit_NotFound() {
        when(budgetCategoryLimitRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> budgetCategoryLimitService.deleteBudgetCategoryLimit(1));
    }
}