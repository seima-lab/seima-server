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
    @Mock private NotificationService notificationService;

    private MockedStatic<UserUtils> userUtilsMockedStatic;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1);
        user.setUserFullName("Test User");
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

    @Test
    void testUpdateTransaction_WithGroup_SendsNotification() {
        // Arrange
        Integer transactionId = 1;
        Integer groupId = 100;
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setGroupId(groupId);
        request.setCategoryId(1);
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrencyCode("USD");
        request.setDescription("Updated transaction");

        Transaction existingTransaction = new Transaction();
        existingTransaction.setTransactionId(transactionId);
        existingTransaction.setUser(user);
        existingTransaction.setAmount(BigDecimal.valueOf(50));
        existingTransaction.setTransactionDate(LocalDateTime.now());
        existingTransaction.setGroup(new Group());
        existingTransaction.getGroup().setGroupId(groupId);

        Category category = new Category();
        category.setCategoryId(1);
        category.setCategoryName("Food");

        Group group = new Group();
        group.setGroupId(groupId);

        Transaction updatedTransaction = new Transaction();
        updatedTransaction.setTransactionId(transactionId);
        updatedTransaction.setGroup(group);
        updatedTransaction.setDescription("Updated transaction");

        TransactionResponse response = new TransactionResponse();

        // Mock repository calls
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(existingTransaction));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByUserUserIdAndGroupGroupId(user.getUserId(), groupId)).thenReturn(true);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(updatedTransaction);
        when(transactionMapper.toResponse(updatedTransaction)).thenReturn(response);

        // Act
        TransactionResponse result = transactionService.updateTransaction(transactionId, request);

        // Assert
        assertNotNull(result);
        verify(notificationService, times(1)).sendNotificationToGroupMembersExceptUser(
            eq(groupId),
            eq(user.getUserId()),
            eq(user.getUserFullName()),
            eq(NotificationType.TRANSACTION_UPDATED),
            eq("Transaction Updated"),
            contains("updated a transaction"),
            contains("seimaapp://groups/" + groupId + "/transactions/" + transactionId)
        );
    }

    @Test
    void testUpdateTransaction_WithoutGroup_DoesNotSendNotification() {
        // Arrange
        Integer transactionId = 1;
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setWalletId(1);
        request.setCategoryId(1);
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrencyCode("USD");

        Transaction existingTransaction = new Transaction();
        existingTransaction.setTransactionId(transactionId);
        existingTransaction.setUser(user);
        existingTransaction.setAmount(BigDecimal.valueOf(50));
        existingTransaction.setTransactionDate(LocalDateTime.now());
        existingTransaction.setWallet(new Wallet());

        Category category = new Category();
        category.setCategoryId(1);

        Wallet wallet = new Wallet();
        wallet.setId(1);

        Transaction updatedTransaction = new Transaction();
        updatedTransaction.setTransactionId(transactionId);
        updatedTransaction.setWallet(wallet);

        TransactionResponse response = new TransactionResponse();

        // Mock repository calls
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(existingTransaction));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(walletRepository.findById(1)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(updatedTransaction);
        when(transactionMapper.toResponse(updatedTransaction)).thenReturn(response);

        // Act
        TransactionResponse result = transactionService.updateTransaction(transactionId, request);

        // Assert
        assertNotNull(result);
        verify(notificationService, never()).sendNotificationToGroupMembersExceptUser(
            any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void testUpdateTransaction_NotificationServiceThrowsException_DoesNotAffectMainFlow() {
        // Arrange
        Integer transactionId = 1;
        Integer groupId = 100;
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setGroupId(groupId);
        request.setCategoryId(1);
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrencyCode("USD");

        Transaction existingTransaction = new Transaction();
        existingTransaction.setTransactionId(transactionId);
        existingTransaction.setUser(user);
        existingTransaction.setAmount(BigDecimal.valueOf(50));
        existingTransaction.setTransactionDate(LocalDateTime.now());
        existingTransaction.setGroup(new Group());
        existingTransaction.getGroup().setGroupId(groupId);

        Category category = new Category();
        category.setCategoryId(1);

        Group group = new Group();
        group.setGroupId(groupId);

        Transaction updatedTransaction = new Transaction();
        updatedTransaction.setTransactionId(transactionId);
        updatedTransaction.setGroup(group);

        TransactionResponse response = new TransactionResponse();

        // Mock repository calls
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(existingTransaction));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByUserUserIdAndGroupGroupId(user.getUserId(), groupId)).thenReturn(true);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(updatedTransaction);
        when(transactionMapper.toResponse(updatedTransaction)).thenReturn(response);

        // Mock notification service to throw exception
        doThrow(new RuntimeException("Notification service error"))
            .when(notificationService).sendNotificationToGroupMembersExceptUser(
                any(), any(), any(), any(), any(), any(), any()
            );

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> transactionService.updateTransaction(transactionId, request));
        
        // Verify transaction was still updated
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testUpdateTransaction_WithNullDescription_UsesDefaultMessage() {
        // Arrange
        Integer transactionId = 1;
        Integer groupId = 100;
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setGroupId(groupId);
        request.setCategoryId(1);
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrencyCode("USD");
        request.setDescription(null); // Null description

        Transaction existingTransaction = new Transaction();
        existingTransaction.setTransactionId(transactionId);
        existingTransaction.setUser(user);
        existingTransaction.setAmount(BigDecimal.valueOf(50));
        existingTransaction.setTransactionDate(LocalDateTime.now());
        existingTransaction.setGroup(new Group());
        existingTransaction.getGroup().setGroupId(groupId);

        Category category = new Category();
        category.setCategoryId(1);

        Group group = new Group();
        group.setGroupId(groupId);

        Transaction updatedTransaction = new Transaction();
        updatedTransaction.setTransactionId(transactionId);
        updatedTransaction.setGroup(group);
        updatedTransaction.setDescription(null);

        TransactionResponse response = new TransactionResponse();

        // Mock repository calls
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(existingTransaction));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByUserUserIdAndGroupGroupId(user.getUserId(), groupId)).thenReturn(true);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(updatedTransaction);
        when(transactionMapper.toResponse(updatedTransaction)).thenReturn(response);

        // Act
        transactionService.updateTransaction(transactionId, request);

        // Assert
        verify(notificationService, times(1)).sendNotificationToGroupMembersExceptUser(
            eq(groupId),
            eq(user.getUserId()),
            eq(user.getUserFullName()),
            eq(NotificationType.TRANSACTION_UPDATED),
            eq("Transaction Updated"),
            contains("updated a transaction"),
            any()
        );
    }

    @Test
    void testDeleteTransaction_WithGroup_SendsNotification() {
        // Arrange
        Integer transactionId = 1;
        Integer groupId = 100;
        
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setUser(user);
        transaction.setAmount(BigDecimal.valueOf(100));
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setDescription("Test transaction to delete");
        transaction.setGroup(new Group());
        transaction.getGroup().setGroupId(groupId);

        Group group = new Group();
        group.setGroupId(groupId);

        // Mock repository calls
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByUserUserIdAndGroupGroupId(user.getUserId(), groupId)).thenReturn(true);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        // Act
        transactionService.deleteTransaction(transactionId);

        // Assert
        verify(notificationService, times(1)).sendNotificationToGroupMembersExceptUser(
            eq(groupId),
            eq(user.getUserId()),
            eq(user.getUserFullName()),
            eq(NotificationType.TRANSACTION_DELETED),
            eq("Transaction Removed"),
            contains("removed a transaction"),
            contains("seimaapp://groups/" + groupId + "/transactions")
        );
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testDeleteTransaction_WithoutGroup_DoesNotSendNotification() {
        // Arrange
        Integer transactionId = 1;
        
        Wallet wallet = new Wallet();
        wallet.setCurrentBalance(BigDecimal.valueOf(1000));
        
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setUser(user);
        transaction.setAmount(BigDecimal.valueOf(100));
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setWallet(wallet);

        // Mock repository calls
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        // Act
        transactionService.deleteTransaction(transactionId);

        // Assert
        verify(notificationService, never()).sendNotificationToGroupMembersExceptUser(
            any(), any(), any(), any(), any(), any(), any()
        );
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testDeleteTransaction_NotificationServiceThrowsException_DoesNotAffectMainFlow() {
        // Arrange
        Integer transactionId = 1;
        Integer groupId = 100;
        
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setUser(user);
        transaction.setAmount(BigDecimal.valueOf(100));
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setGroup(new Group());
        transaction.getGroup().setGroupId(groupId);

        Group group = new Group();
        group.setGroupId(groupId);

        // Mock repository calls
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByUserUserIdAndGroupGroupId(user.getUserId(), groupId)).thenReturn(true);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        // Mock notification service to throw exception
        doThrow(new RuntimeException("Notification service error"))
            .when(notificationService).sendNotificationToGroupMembersExceptUser(
                any(), any(), any(), any(), any(), any(), any()
            );

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> transactionService.deleteTransaction(transactionId));
        
        // Verify transaction was still deleted
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testDeleteTransaction_WithNullDescription_UsesDefaultMessage() {
        // Arrange
        Integer transactionId = 1;
        Integer groupId = 100;
        
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setUser(user);
        transaction.setAmount(BigDecimal.valueOf(100));
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setDescription(null); // Null description
        transaction.setGroup(new Group());
        transaction.getGroup().setGroupId(groupId);

        Group group = new Group();
        group.setGroupId(groupId);

        // Mock repository calls
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByUserUserIdAndGroupGroupId(user.getUserId(), groupId)).thenReturn(true);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        // Act
        transactionService.deleteTransaction(transactionId);

        // Assert
        verify(notificationService, times(1)).sendNotificationToGroupMembersExceptUser(
            eq(groupId),
            eq(user.getUserId()),
            eq(user.getUserFullName()),
            eq(NotificationType.TRANSACTION_DELETED),
            eq("Transaction Removed"),
            contains("removed a transaction"),
            any()
        );
    }

    @Test
    void testDeleteTransaction_TransactionNotFound_ThrowsException() {
        // Arrange
        Integer transactionId = 999;
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> transactionService.deleteTransaction(transactionId));
        verify(notificationService, never()).sendNotificationToGroupMembersExceptUser(
            any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void testSaveTransaction_WithGroup_SendsNotification() {
        // Arrange
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setGroupId(100);
        request.setCategoryId(1);
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrencyCode("USD");
        request.setDescription("Test transaction");

        Category category = new Category();
        category.setCategoryId(1);
        category.setCategoryName("Food");

        Group group = new Group();
        group.setGroupId(100);

        Transaction transaction = new Transaction();
        transaction.setTransactionId(1);
        transaction.setGroup(group);
        transaction.setDescription("Test transaction");
        transaction.setTransactionType(TransactionType.EXPENSE);
        transaction.setAmount(BigDecimal.valueOf(100));
        transaction.setCurrencyCode("USD");

        Transaction savedTransaction = new Transaction();
        savedTransaction.setTransactionId(1);
        savedTransaction.setGroup(group);
        savedTransaction.setDescription("Test transaction");
        savedTransaction.setTransactionType(TransactionType.EXPENSE);
        savedTransaction.setAmount(BigDecimal.valueOf(100));
        savedTransaction.setCurrencyCode("USD");

        TransactionResponse response = new TransactionResponse();

        // Mock repository calls
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(groupRepository.findById(100)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByUserUserIdAndGroupGroupId(user.getUserId(), 100)).thenReturn(true);
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toResponse(savedTransaction)).thenReturn(response);

        // Act
        TransactionResponse result = transactionService.saveTransaction(request, TransactionType.EXPENSE);

        // Assert
        assertNotNull(result);
        verify(notificationService, times(1)).sendNotificationToGroupMembersExceptUser(
            eq(100),
            eq(user.getUserId()),
            eq(user.getUserFullName()),
            eq(NotificationType.TRANSACTION_CREATED),
            eq("New Transaction"),
            contains("added a new expense transaction: 100 USD"),
            contains("seimaapp://groups/100/transactions/1")
        );
    }

    @Test
    void testSaveTransaction_WithoutGroup_DoesNotSendNotification() {
        // Arrange
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setWalletId(1);
        request.setCategoryId(1);
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrencyCode("USD");
        request.setTransactionDate(LocalDateTime.now());

        Category category = new Category();
        category.setCategoryId(1);

        Wallet wallet = new Wallet();
        wallet.setId(1);

        Transaction transaction = new Transaction();
        transaction.setTransactionId(1);
        transaction.setWallet(wallet);
        transaction.setTransactionDate(LocalDateTime.now());

        Transaction savedTransaction = new Transaction();
        savedTransaction.setTransactionId(1);
        savedTransaction.setWallet(wallet);

        TransactionResponse response = new TransactionResponse();

        // Mock repository calls
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(walletRepository.findById(1)).thenReturn(Optional.of(wallet));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toResponse(savedTransaction)).thenReturn(response);

        // Act
        TransactionResponse result = transactionService.saveTransaction(request, TransactionType.EXPENSE);

        // Assert
        assertNotNull(result);
        verify(notificationService, never()).sendNotificationToGroupMembersExceptUser(
            any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void testSaveTransaction_NotificationServiceThrowsException_DoesNotAffectMainFlow() {
        // Arrange
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setGroupId(100);
        request.setCategoryId(1);
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrencyCode("USD");

        Category category = new Category();
        category.setCategoryId(1);

        Group group = new Group();
        group.setGroupId(100);

        Transaction transaction = new Transaction();
        transaction.setTransactionId(1);
        transaction.setGroup(group);
        transaction.setTransactionType(TransactionType.EXPENSE);
        transaction.setAmount(BigDecimal.valueOf(100));

        Transaction savedTransaction = new Transaction();
        savedTransaction.setTransactionId(1);
        savedTransaction.setGroup(group);
        savedTransaction.setTransactionType(TransactionType.EXPENSE);
        savedTransaction.setAmount(BigDecimal.valueOf(100));

        TransactionResponse response = new TransactionResponse();

        // Mock repository calls
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(groupRepository.findById(100)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByUserUserIdAndGroupGroupId(user.getUserId(), 100)).thenReturn(true);
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toResponse(savedTransaction)).thenReturn(response);

        // Mock notification service to throw exception
        doThrow(new RuntimeException("Notification service error"))
            .when(notificationService).sendNotificationToGroupMembersExceptUser(
                any(), any(), any(), any(), any(), any(), any()
            );

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> transactionService.saveTransaction(request, TransactionType.EXPENSE));
        
        // Verify transaction was still created
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testSaveTransaction_WithNullDescription_UsesDefaultMessage() {
        // Arrange
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setGroupId(100);
        request.setCategoryId(1);
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrencyCode("USD");
        request.setDescription(null); // Null description

        Category category = new Category();
        category.setCategoryId(1);

        Group group = new Group();
        group.setGroupId(100);

        Transaction transaction = new Transaction();
        transaction.setTransactionId(1);
        transaction.setGroup(group);
        transaction.setDescription(null);
        transaction.setTransactionType(TransactionType.EXPENSE);
        transaction.setAmount(BigDecimal.valueOf(100));
        transaction.setCurrencyCode("USD");

        Transaction savedTransaction = new Transaction();
        savedTransaction.setTransactionId(1);
        savedTransaction.setGroup(group);
        savedTransaction.setDescription(null);
        savedTransaction.setTransactionType(TransactionType.EXPENSE);
        savedTransaction.setAmount(BigDecimal.valueOf(100));
        savedTransaction.setCurrencyCode("USD");

        TransactionResponse response = new TransactionResponse();

        // Mock repository calls
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(groupRepository.findById(100)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByUserUserIdAndGroupGroupId(user.getUserId(), 100)).thenReturn(true);
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toResponse(savedTransaction)).thenReturn(response);

        // Act
        transactionService.saveTransaction(request, TransactionType.EXPENSE);

        // Assert
        verify(notificationService, times(1)).sendNotificationToGroupMembersExceptUser(
            eq(100),
            eq(user.getUserId()),
            eq(user.getUserFullName()),
            eq(NotificationType.TRANSACTION_CREATED),
            eq("New Transaction"),
            contains("added a new expense transaction: 100 USD"),
            any()
        );
    }

    @Test
    void testRecordExpense_WithGroup_SendsNotification() {
        // Arrange
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setGroupId(100);
        request.setCategoryId(1);
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrencyCode("USD");
        request.setDescription("Expense transaction");

        Category category = new Category();
        category.setCategoryId(1);

        Group group = new Group();
        group.setGroupId(100);

        Transaction transaction = new Transaction();
        transaction.setTransactionId(1);
        transaction.setGroup(group);
        transaction.setDescription("Expense transaction");
        transaction.setTransactionType(TransactionType.EXPENSE);
        transaction.setAmount(BigDecimal.valueOf(100));
        transaction.setCurrencyCode("USD");

        Transaction savedTransaction = new Transaction();
        savedTransaction.setTransactionId(1);
        savedTransaction.setGroup(group);
        savedTransaction.setDescription("Expense transaction");
        savedTransaction.setTransactionType(TransactionType.EXPENSE);
        savedTransaction.setAmount(BigDecimal.valueOf(100));
        savedTransaction.setCurrencyCode("USD");

        TransactionResponse response = new TransactionResponse();

        // Mock repository calls
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(groupRepository.findById(100)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByUserUserIdAndGroupGroupId(user.getUserId(), 100)).thenReturn(true);
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toResponse(savedTransaction)).thenReturn(response);

        // Act
        TransactionResponse result = transactionService.recordExpense(request);

        // Assert
        assertNotNull(result);
        verify(notificationService, times(1)).sendNotificationToGroupMembersExceptUser(
            eq(100),
            eq(user.getUserId()),
            eq(user.getUserFullName()),
            eq(NotificationType.TRANSACTION_CREATED),
            eq("New Transaction"),
            contains("added a new expense transaction: 100 USD"),
            any()
        );
    }

    @Test
    void testRecordIncome_WithGroup_SendsNotification() {
        // Arrange
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setGroupId(100);
        request.setCategoryId(1);
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrencyCode("USD");
        request.setDescription("Income transaction");

        Category category = new Category();
        category.setCategoryId(1);

        Group group = new Group();
        group.setGroupId(100);

        Transaction transaction = new Transaction();
        transaction.setTransactionId(1);
        transaction.setGroup(group);
        transaction.setDescription("Income transaction");
        transaction.setTransactionType(TransactionType.INCOME);
        transaction.setAmount(BigDecimal.valueOf(100));
        transaction.setCurrencyCode("USD");

        Transaction savedTransaction = new Transaction();
        savedTransaction.setTransactionId(1);
        savedTransaction.setGroup(group);
        savedTransaction.setDescription("Income transaction");
        savedTransaction.setTransactionType(TransactionType.INCOME);
        savedTransaction.setAmount(BigDecimal.valueOf(100));
        savedTransaction.setCurrencyCode("USD");

        TransactionResponse response = new TransactionResponse();

        // Mock repository calls
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(groupRepository.findById(100)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByUserUserIdAndGroupGroupId(user.getUserId(), 100)).thenReturn(true);
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toResponse(savedTransaction)).thenReturn(response);

        // Act
        TransactionResponse result = transactionService.recordIncome(request);

        // Assert
        assertNotNull(result);
        verify(notificationService, times(1)).sendNotificationToGroupMembersExceptUser(
            eq(100),
            eq(user.getUserId()),
            eq(user.getUserFullName()),
            eq(NotificationType.TRANSACTION_CREATED),
            eq("New Transaction"),
            contains("added a new income transaction: 100 USD"),
            any()
        );
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