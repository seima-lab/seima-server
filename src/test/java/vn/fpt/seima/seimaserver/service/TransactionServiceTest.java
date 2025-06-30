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

    @Mock private TransactionRepository transactionRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionMapper transactionMapper;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private WalletService walletService;
    @Mock private BudgetService budgetService;

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
    void testGetAllTransaction() {
        // Arrange
        Transaction transaction = new Transaction();
        transaction.setTransactionId(1); // nếu Transaction có ID
        TransactionResponse expectedResponse = new TransactionResponse();
        expectedResponse.setTransactionId(1); // nếu có field id

        Page<Transaction> mockPage = new PageImpl<>(List.of(transaction));

        when(transactionRepository.findAll(any(Pageable.class))).thenReturn(mockPage);
        when(transactionMapper.toResponse(transaction)).thenReturn(expectedResponse);

        // Act
        Page<TransactionResponse> result = transactionService.getAllTransaction(Pageable.unpaged());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(expectedResponse.getTransactionId(), result.getContent().get(0).getTransactionId());

        // Verify interactions
        verify(transactionRepository).findAll(any(Pageable.class));
        verify(transactionMapper).toResponse(transaction);
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
    void testSaveTransaction_Success() throws Exception {
        // Arrange
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setWalletId(1);
        request.setCategoryId(2);
        request.setAmount(BigDecimal.valueOf(100));
        request.setReceiptImageUrl("fake-base64-image");

        Wallet wallet = new Wallet();
        wallet.setId(1);

        Category category = new Category();
        category.setCategoryId(2);

        Transaction transaction = new Transaction();
        Transaction savedTransaction = new Transaction();
        savedTransaction.setTransactionId(99); // giả sử ID được sinh khi save

        TransactionResponse expectedResponse = new TransactionResponse();
        expectedResponse.setTransactionId(99);

        when(walletRepository.findById(1)).thenReturn(Optional.of(wallet));
        when(categoryRepository.findById(2)).thenReturn(Optional.of(category));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(savedTransaction);
        when(transactionMapper.toResponse(savedTransaction)).thenReturn(expectedResponse);

        // Act
        TransactionResponse result = transactionService.saveTransaction(request, TransactionType.EXPENSE);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse.getTransactionId(), result.getTransactionId());

        // Optional: verify được gọi đúng
        verify(walletRepository).findById(1);
        verify(categoryRepository).findById(2);
        verify(transactionRepository).save(transaction);
        verify(transactionMapper).toEntity(request);
        verify(transactionMapper).toResponse(savedTransaction);
    }


    @Test
    void testUpdateTransaction_Success() throws Exception {
        // Arrange
        int transactionId = 1;

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setWalletId(1);
        request.setCategoryId(2);
        request.setAmount(BigDecimal.valueOf(200));
        request.setReceiptImageUrl("fake-base64-image");

        Transaction existingTransaction = new Transaction();
        Wallet wallet = new Wallet();
        Category category = new Category();
        Transaction updatedTransaction = new Transaction();
        updatedTransaction.setTransactionId(transactionId);

        TransactionResponse expectedResponse = new TransactionResponse();
        expectedResponse.setTransactionId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(existingTransaction));
        when(walletRepository.findById(1)).thenReturn(Optional.of(wallet));
        when(categoryRepository.findById(2)).thenReturn(Optional.of(category));

        doNothing().when(transactionMapper).updateTransactionFromDto(request, existingTransaction);
        when(transactionRepository.save(existingTransaction)).thenReturn(updatedTransaction);
        when(transactionMapper.toResponse(updatedTransaction)).thenReturn(expectedResponse);

        // Act
        TransactionResponse result = transactionService.updateTransaction(transactionId, request);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse.getTransactionId(), result.getTransactionId());

        // Verify interactions
        verify(transactionRepository).findById(transactionId);
        verify(walletRepository).findById(1);
        verify(categoryRepository).findById(2);
        verify(transactionMapper).updateTransactionFromDto(request, existingTransaction);
        verify(transactionRepository).save(existingTransaction);
        verify(transactionMapper).toResponse(updatedTransaction);
    }


    @Test
    void testDeleteTransaction_Success() {
        Transaction transaction = new Transaction();

        when(transactionRepository.findById(1)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        transactionService.deleteTransaction(1);

        assertEquals(TransactionType.INACTIVE, transaction.getTransactionType());
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

