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
    @Mock private RedisService redisService;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository groupMemberRepository;

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


//    @Test
//    void saveTransaction_Income_WalletPath_Success() {
//        // Arrange
//        CreateTransactionRequest request = new CreateTransactionRequest();
//        request.setAmount(BigDecimal.TEN);
//        request.setWalletId(1);
//        request.setCategoryId(2);
//        request.setCurrencyCode("VND");
//        request.setTransactionDate(LocalDateTime.of(2024, 7, 15, 10, 30));
//        // request.setGroupId(null) // mặc định null
//
//        User user = new User();
//        user.setUserId(999);
//
//        Wallet wallet = new Wallet();
//        wallet.setId(1);
//
//        Category category = new Category();
//        category.setCategoryId(2);
//
//        Transaction transaction = new Transaction();
//        transaction.setTransactionDate(request.getTransactionDate());
//        transaction.setAmount(request.getAmount());
//        transaction.setTransactionType(TransactionType.INCOME);
//        transaction.setUser(user);
//        transaction.setWallet(wallet);
//        transaction.setCategory(category);
//
//        Transaction saved = new Transaction();
//        saved.setTransactionDate(request.getTransactionDate());
//        saved.setAmount(request.getAmount());
//        saved.setTransactionType(TransactionType.INCOME);
//        saved.setUser(user);
//        saved.setWallet(wallet);
//        saved.setCategory(category);
//
//        TransactionResponse expectedResponse = new TransactionResponse();
//
//        // Mock static UserUtils.getCurrentUser()
//        try (MockedStatic<UserUtils> mocked = mockStatic(UserUtils.class)) {
//            mocked.when(UserUtils::getCurrentUser).thenReturn(user);
//
//            // Mocks
//            when(categoryRepository.findById(2)).thenReturn(Optional.of(category));
//            when(walletRepository.findById(1)).thenReturn(Optional.of(wallet));
//            when(transactionMapper.toEntity(request)).thenReturn(transaction);
//            when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);
//            when(transactionMapper.toResponse(saved)).thenReturn(expectedResponse);
//
//            // Tính key cache mong đợi
//            YearMonth ym = YearMonth.from(request.getTransactionDate());
//            String expectedCacheKey = String.format("tx:overview:%d:%s", user.getUserId(), ym);
//
//            // Act
//            TransactionResponse response = transactionService.saveTransaction(request, TransactionType.INCOME);
//
//            // Assert
//            assertNotNull(response);
//            assertEquals(expectedResponse, response);
//
//            // Verify map + save
//            verify(transactionMapper).toEntity(request);
//            verify(transactionRepository).save(any(Transaction.class));
//            verify(transactionMapper).toResponse(saved);
//
//            // Verify gọi giảm ngân sách & ví cho INCOME (đúng theo code hiện tại)
//            verify(budgetService).reduceAmount(
//                    eq(user.getUserId()),
//                    eq(2),
//                    eq(BigDecimal.TEN),
//                    eq(request.getTransactionDate()),
//                    eq("INCOME"),
//                    eq("VND")
//            );
//            verify(walletService).reduceAmount(
//                    eq(1),
//                    eq(BigDecimal.TEN),
//                    eq("INCOME"),
//                    eq("VND")
//            );
//
//            // Verify XÓA CACHE đúng key
//            verify(redisService).delete(eq(expectedCacheKey));
//
//            // Không đụng group
//            verifyNoInteractions(groupRepository, groupMemberRepository);
//        }
//    }
//
//    @Test
//    void testUpdateTransaction_AmountIncreased() {
//        CreateTransactionRequest request = new CreateTransactionRequest();
//        request.setWalletId(1);
//        request.setCategoryId(2);
//        request.setAmount(new BigDecimal("200"));
//        request.setCurrencyCode("VND");
//
//        Wallet wallet = new Wallet();
//        Category category = new Category();
//        Transaction oldTransaction = new Transaction();
//        oldTransaction.setAmount(new BigDecimal("100"));
//        oldTransaction.setTransactionDate(LocalDateTime.now());
//        oldTransaction.setUser(user);
//
//        when(transactionRepository.findById(1)).thenReturn(Optional.of(oldTransaction));
//        when(walletRepository.findById(1)).thenReturn(Optional.of(wallet));
//        when(categoryRepository.findById(2)).thenReturn(Optional.of(category));
//        when(transactionRepository.save(any())).thenReturn(oldTransaction);
//        when(transactionMapper.toResponse(any())).thenReturn(new TransactionResponse());
//
//        TransactionResponse result = transactionService.updateTransaction(1, request);
//
//        assertNotNull(result);
//        verify(walletService).reduceAmount(1, new BigDecimal("100"), "update-subtract", "VND");
//        verify(budgetService).reduceAmount(user.getUserId(), 2, new BigDecimal("100"), oldTransaction.getTransactionDate(), "update-subtract", "VND");
//    }
}