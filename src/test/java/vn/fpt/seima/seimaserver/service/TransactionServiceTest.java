package vn.fpt.seima.seimaserver.service;



import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.fpt.seima.seimaserver.dto.request.transaction.CreateTransactionRequest;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionCategoryReportResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionDetailReportResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionOverviewResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.mapper.TransactionMapper;
import vn.fpt.seima.seimaserver.repository.*;
import vn.fpt.seima.seimaserver.service.impl.TransactionServiceImpl;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionMapper transactionMapper;
    @Mock private CacheManager cacheManager;
    @Mock private WalletService walletService;
    @Mock private BudgetService budgetService;
    @Mock
    private Cache cache;
    @Mock
    private BudgetRepository budgetRepository;
    @InjectMocks private TransactionServiceImpl transactionService;
    @Mock
    private BudgetCategoryLimitRepository budgetCategoryLimitRepository;

    private MockedStatic<UserUtils> userUtilsMockedStatic;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1);
        userUtilsMockedStatic = mockStatic(UserUtils.class);
        userUtilsMockedStatic.when(UserUtils::getCurrentUser).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        if (userUtilsMockedStatic != null) {
            userUtilsMockedStatic.close();
        }
    }
    @Test
    void testGetTransactionById_Found() {
        Transaction transaction = new Transaction();
        TransactionResponse response = new TransactionResponse();

        when(transactionRepository.findById(1)).thenReturn(Optional.of(transaction));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        TransactionResponse result = transactionService.getTransactionById(1);
        assertEquals(response, result);
    }

    @Test
    void testGetTransactionById_NotFound() {
        when(transactionRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> transactionService.getTransactionById(1));
    }
    @Test
    void testDeleteTransaction_Success() {
        // Arrange
        int transactionId = 1;

        // Wallet
        Wallet wallet = new Wallet();
        wallet.setCurrentBalance(new BigDecimal("1000"));

        // User
        User user = new User();
        user.setUserId(123);

        // Category
        Category category = new Category();
        category.setCategoryId(10);

        // Transaction (ngày 15/07/2025)
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setTransactionDate(LocalDateTime.of(2025, 7, 15, 0, 0));
        transaction.setTransactionType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal("200"));
        transaction.setWallet(wallet);
        transaction.setUser(user);
        transaction.setCategory(category);

        // Budget (start < transactionDate < end)
        Budget budget = new Budget();
        budget.setBudgetId(1);
        budget.setStartDate(LocalDateTime.of(2025, 6, 15, 0, 0));
        budget.setEndDate(LocalDateTime.of(2025, 8, 15, 0, 0));
        budget.setBudgetRemainingAmount(new BigDecimal("500"));

        // BudgetCategoryLimit
        BudgetCategoryLimit bcl = new BudgetCategoryLimit();
        bcl.setBudget(budget);

        // Mock các repository và cache
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(cacheManager.getCache("transactionOverview")).thenReturn(cache);
        when(budgetCategoryLimitRepository.findByTransaction(category.getCategoryId())).thenReturn(List.of(bcl));
        when(budgetRepository.findById(budget.getBudgetId())).thenReturn(Optional.of(budget));

        // Act
        transactionService.deleteTransaction(transactionId);

        // Assert
        assertEquals(TransactionType.INACTIVE, transaction.getTransactionType());
        assertEquals(new BigDecimal("1200"), wallet.getCurrentBalance()); // Hoàn tiền chi tiêu
        assertEquals(new BigDecimal("700"), budget.getBudgetRemainingAmount()); // Cộng lại vào ngân sách

        verify(cache).evict("123-2025-07");
        verify(walletRepository).save(wallet);
        verify(transactionRepository).save(transaction);
        verify(budgetRepository).save(budget);
    }






    @Test
    void testRecordIncome() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        TransactionServiceImpl spyService = spy(transactionService);
        TransactionResponse response = new TransactionResponse();

        doReturn(response).when(spyService).saveTransaction(request, TransactionType.INCOME);
        TransactionResponse result = spyService.recordIncome(request);

        assertEquals(response, result);
    }

    @Test
    void testRecordExpense() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        TransactionServiceImpl spyService = spy(transactionService);
        TransactionResponse response = new TransactionResponse();

        doReturn(response).when(spyService).saveTransaction(request, TransactionType.EXPENSE);
        TransactionResponse result = spyService.recordExpense(request);

        assertEquals(response, result);
    }

    @Test
    void testTransferTransaction() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        TransactionServiceImpl spyService = spy(transactionService);
        TransactionResponse response = new TransactionResponse();

        doReturn(response).when(spyService).saveTransaction(request, TransactionType.TRANSFER);
        TransactionResponse result = spyService.transferTransaction(request);

        assertEquals(response, result);
    }

    @Test
    void testGetTransactionOverview() {
        Transaction transaction = new Transaction();
        User currentUser = UserUtils.getCurrentUser();
        transaction.setTransactionType(TransactionType.INCOME);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setAmount(BigDecimal.valueOf(100));

        when(transactionRepository.findAllByUserAndTransactionDateBetween(any(), any(), any()))
                .thenReturn(List.of(transaction));


        TransactionOverviewResponse result = transactionService.getTransactionOverview(currentUser.getUserId(), YearMonth.now());
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(100), result.getSummary().getTotalIncome());
    }
    @Test
    void testRecordIncome_Success() {
        // Arrange
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(BigDecimal.TEN);
        request.setWalletId(1);
        request.setCategoryId(2);
        request.setCurrencyCode("VND");
        request.setTransactionDate(LocalDateTime.of(2024, 7, 15, 10, 30));

        Wallet wallet = new Wallet();
        wallet.setId(1);

        Category category = new Category();
        category.setCategoryId(2);

        Transaction transaction = new Transaction();
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setAmount(request.getAmount());
        transaction.setTransactionType(TransactionType.INCOME);
        transaction.setUser(user);
        transaction.setWallet(wallet);
        transaction.setCategory(category);

        TransactionResponse expectedResponse = new TransactionResponse();

        when(walletRepository.findById(1)).thenReturn(Optional.of(wallet));
        when(categoryRepository.findById(2)).thenReturn(Optional.of(category));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(any())).thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(expectedResponse);

        // Act
        TransactionResponse response = transactionService.recordIncome(request);

        // Assert
        assertNotNull(response);
        assertEquals(expectedResponse, response);
        verify(walletService).reduceAmount(1, BigDecimal.TEN, "INCOME", "VND");
        verify(budgetService).reduceAmount(1, 2, BigDecimal.TEN, request.getTransactionDate(), "INCOME", "VND");
    }

    @Test
    void testUpdateTransaction_AmountIncreased() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setWalletId(1);
        request.setCategoryId(2);
        request.setAmount(new BigDecimal("200"));
        request.setCurrencyCode("VND");

        Wallet wallet = new Wallet();
        Category category = new Category();
        Transaction oldTransaction = new Transaction();
        oldTransaction.setAmount(new BigDecimal("100"));
        oldTransaction.setTransactionDate(LocalDateTime.now());
        oldTransaction.setUser(user);

        when(transactionRepository.findById(1)).thenReturn(Optional.of(oldTransaction));
        when(walletRepository.findById(1)).thenReturn(Optional.of(wallet));
        when(categoryRepository.findById(2)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any())).thenReturn(oldTransaction);
        when(transactionMapper.toResponse(any())).thenReturn(new TransactionResponse());

        TransactionResponse result = transactionService.updateTransaction(1, request);

        assertNotNull(result);
        verify(walletService).reduceAmount(1, new BigDecimal("100"), "update-subtract", "VND");
        verify(budgetService).reduceAmount(user.getUserId(), 2, new BigDecimal("100"), oldTransaction.getTransactionDate(), "update-subtract", "VND");
    }
    @Test
    void testGetCategoryReportDetail_Success() {
        LocalDate from = LocalDate.of(2025, 7, 1);
        LocalDate to = LocalDate.of(2025, 7, 5);

        Transaction tx = new Transaction();
        tx.setTransactionType(TransactionType.EXPENSE);
        tx.setTransactionDate(LocalDateTime.of(2025, 7, 3, 10, 0));
        tx.setAmount(BigDecimal.TEN);
        Category cat = new Category();
        cat.setCategoryId(1);
        cat.setCategoryName("Food");
        cat.setCategoryIconUrl("icon.png");
        tx.setCategory(cat);

        when(transactionRepository.findExpensesByUserAndDateRange(eq(1), eq(user.getUserId()), any(), any()))
                .thenReturn(List.of(tx));

        TransactionDetailReportResponse response = transactionService.getCategoryReportDetail(1, from, to);

        assertNotNull(response);
        assertEquals(BigDecimal.TEN, response.getTotalExpense());
        assertTrue(response.getData().containsKey("2025-07-03"));
    }
    @Test
    void testGetCategoryReport_Monthly() {
        LocalDate from = LocalDate.of(2025, 7, 1);
        LocalDate to = LocalDate.of(2025, 7, 31);

        Transaction tx = new Transaction();
        tx.setTransactionType(TransactionType.EXPENSE);
        tx.setTransactionDate(LocalDateTime.of(2025, 7, 10, 10, 0));
        tx.setAmount(new BigDecimal("100"));
        Category cat = new Category();
        cat.setCategoryId(2);
        cat.setCategoryName("Travel");
        cat.setCategoryIconUrl("travel.png");
        tx.setCategory(cat);

        when(transactionRepository.findExpensesByUserAndDateRange(eq(2), eq(user.getUserId()), any(), any()))
                .thenReturn(List.of(tx));

        TransactionCategoryReportResponse response = transactionService.getCategoryReport(
                PeriodType.MONTHLY, 2, from, to);

        assertNotNull(response);
        assertEquals(new BigDecimal("100"), response.getTotalExpense());
        assertEquals(1, response.getData().size());
    }

}

