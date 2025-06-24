package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
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
    @Mock
    private WalletService walletService;  // Thêm vào đây
    @Mock
    private BudgetService budgetService;

    @Mock private TransactionRepository transactionRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionMapper transactionMapper;

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
    void getAllTransaction_WhenCalled_ReturnsPage() {
        // Given
        Transaction transaction = new Transaction();
        Page<Transaction> page = new PageImpl<>(List.of(transaction));
        TransactionResponse response = new TransactionResponse();

        when(transactionRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        // When
        Page<TransactionResponse> result = transactionService.getAllTransaction(Pageable.unpaged());

        // Then
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getTransactionById_WhenFound_ReturnsResponse() {
        // Given
        Transaction transaction = new Transaction();
        TransactionResponse response = new TransactionResponse();

        when(transactionRepository.findById(1)).thenReturn(Optional.of(transaction));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        // When
        TransactionResponse result = transactionService.getTransactionById(1);

        // Then
        assertEquals(response, result);
    }

    @Test
    void getTransactionById_WhenNotFound_ThrowsException() {
        // Given
        when(transactionRepository.findById(1)).thenReturn(Optional.empty());

        // Then
        assertThrows(IllegalArgumentException.class, () -> transactionService.getTransactionById(1));
    }

    @Test
    void saveTransaction_WhenSuccess_ReturnsResponse() throws Exception {
        // Given
        Mockito.doNothing().when(budgetService).reduceAmount(Mockito.anyInt(), Mockito.any());

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setWalletId(1);
        request.setCategoryId(2);
        request.setAmount(BigDecimal.valueOf(100));
        request.setReceiptImageUrl("fake-base64");

        Wallet wallet = new Wallet();
        Category category = new Category();
        Transaction transaction = new Transaction();
        Transaction savedTransaction = new Transaction();
        TransactionResponse response = new TransactionResponse();

        when(walletRepository.findById(1)).thenReturn(Optional.of(wallet));
        when(categoryRepository.findById(2)).thenReturn(Optional.of(category));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(savedTransaction);
        when(transactionMapper.toResponse(savedTransaction)).thenReturn(response);

        // When
        TransactionResponse result = transactionService.saveTransaction(request, TransactionType.EXPENSE);

        // Then
        assertEquals(response, result);
    }

    @Test
    void updateTransaction_WhenSuccess_ReturnsResponse() throws Exception {
        // Given
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setWalletId(1);
        request.setCategoryId(2);
        request.setAmount(BigDecimal.valueOf(200));
        request.setReceiptImageUrl("fake-base64");

        Transaction transaction = new Transaction();
        Wallet wallet = new Wallet();
        Category category = new Category();
        Transaction updatedTransaction = new Transaction();
        TransactionResponse response = new TransactionResponse();

        when(transactionRepository.findById(1)).thenReturn(Optional.of(transaction));
        when(walletRepository.findById(1)).thenReturn(Optional.of(wallet));
        when(categoryRepository.findById(2)).thenReturn(Optional.of(category));
        doNothing().when(transactionMapper).updateTransactionFromDto(request, transaction);
        when(transactionRepository.save(transaction)).thenReturn(updatedTransaction);
        when(transactionMapper.toResponse(updatedTransaction)).thenReturn(response);

        // When
        TransactionResponse result = transactionService.updateTransaction(1, request);

        // Then
        assertEquals(response, result);
    }

    @Test
    void deleteTransaction_WhenSuccess_SetsInactive() {
        // Given
        Transaction transaction = new Transaction();
        when(transactionRepository.findById(1)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        // When
        transactionService.deleteTransaction(1);

        // Then
        assertEquals(TransactionType.INACTIVE, transaction.getTransactionType());
        verify(transactionRepository).save(transaction);
    }

    @Test
    void recordIncome_WhenCalled_ReturnsResponse() {
        // Given
        CreateTransactionRequest request = new CreateTransactionRequest();
        TransactionResponse response = new TransactionResponse();
        TransactionServiceImpl spyService = spy(transactionService);

        doReturn(response).when(spyService).saveTransaction(request, TransactionType.INCOME);

        // When
        TransactionResponse result = spyService.recordIncome(request);

        // Then
        assertEquals(response, result);
    }

    @Test
    void recordExpense_WhenCalled_ReturnsResponse() {
        // Given
        CreateTransactionRequest request = new CreateTransactionRequest();
        TransactionResponse response = new TransactionResponse();
        TransactionServiceImpl spyService = spy(transactionService);

        doReturn(response).when(spyService).saveTransaction(request, TransactionType.EXPENSE);

        // When
        TransactionResponse result = spyService.recordExpense(request);

        // Then
        assertEquals(response, result);
    }

    @Test
    void transferTransaction_WhenCalled_ReturnsResponse() {
        // Given
        CreateTransactionRequest request = new CreateTransactionRequest();
        TransactionResponse response = new TransactionResponse();
        TransactionServiceImpl spyService = spy(transactionService);

        doReturn(response).when(spyService).saveTransaction(request, TransactionType.TRANSFER);

        // When
        TransactionResponse result = spyService.transferTransaction(request);

        // Then
        assertEquals(response, result);
    }

    @Test
    void getTransactionOverview_WhenCalled_ReturnsSummary() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.INCOME);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setAmount(BigDecimal.valueOf(100));

        when(transactionRepository.findAllByUserAndTransactionDateBetween(any(), any(), any()))
                .thenReturn(List.of(transaction));

        // When
        TransactionOverviewResponse result = transactionService.getTransactionOverview(YearMonth.now());

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(100), result.getSummary().getTotalIncome());
    }
}
