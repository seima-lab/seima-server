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
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionOverviewResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.mapper.TransactionMapper;
import vn.fpt.seima.seimaserver.repository.*;
import vn.fpt.seima.seimaserver.service.impl.TransactionServiceImpl;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.math.BigDecimal;
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

    @InjectMocks private TransactionServiceImpl transactionService;

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
        userUtilsMockedStatic.close();
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
        Transaction transaction = new Transaction();
        User user = new User();
        user.setUserId(1);
        transaction.setUser(user);
        transaction.setTransactionDate(LocalDateTime.of(2025, 7, 6, 10, 0));
        transaction.setTransactionType(TransactionType.EXPENSE);

        Cache mockCache = mock(Cache.class);

        when(transactionRepository.findById(1)).thenReturn(Optional.of(transaction));
        when(cacheManager.getCache("transactionOverview")).thenReturn(mockCache);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        // Act
        transactionService.deleteTransaction(1);

        // Assert
        assertEquals(TransactionType.INACTIVE, transaction.getTransactionType());
        verify(mockCache).evict("1-2025-07");
        verify(transactionRepository).save(transaction);
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
}

