package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
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
        Transaction transaction = new Transaction();
        Page<Transaction> page = new PageImpl<>(List.of(transaction));
        TransactionResponse response = new TransactionResponse();

        when(transactionRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        Page<TransactionResponse> result = transactionService.getAllTransaction(Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
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
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setWalletId(1);
        request.setCategoryId(2);
        request.setAmount(BigDecimal.valueOf(100));
        request.setReceiptImageUrl("fake-base64-image");

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


        TransactionResponse result = transactionService.saveTransaction(request, TransactionType.EXPENSE);
        assertEquals(response, result);
    }

    @Test
    void testUpdateTransaction_Success() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setWalletId(1);
        request.setCategoryId(2);
        request.setAmount(BigDecimal.valueOf(200));
        request.setReceiptImageUrl("fake-base64-image");

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

        TransactionResponse result = transactionService.updateTransaction(1, request);
        assertEquals(response, result);
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
        transaction.setTransactionType(TransactionType.INCOME);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setAmount(BigDecimal.valueOf(100));

        when(transactionRepository.findAllByUserAndTransactionDateBetween(any(), any(), any()))
                .thenReturn(List.of(transaction));

        TransactionOverviewResponse result = transactionService.getTransactionOverview(YearMonth.now());
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(100), result.getSummary().getTotalIncome());
    }
}
