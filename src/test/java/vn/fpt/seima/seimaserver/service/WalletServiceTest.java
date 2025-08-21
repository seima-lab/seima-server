package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.fpt.seima.seimaserver.dto.request.wallet.CreateWalletRequest;
import vn.fpt.seima.seimaserver.dto.response.wallet.WalletResponse;
import vn.fpt.seima.seimaserver.entity.BankInformation;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.entity.Wallet;
import vn.fpt.seima.seimaserver.entity.WalletType;
import vn.fpt.seima.seimaserver.exception.WalletException;
import vn.fpt.seima.seimaserver.mapper.WalletMapper;
import vn.fpt.seima.seimaserver.repository.*;
import vn.fpt.seima.seimaserver.service.impl.WalletServiceImpl;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletTypeRepository walletTypeRepository;

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private BudgetWalletRepository budgetWalletRepository;
    
    @Mock
    private BankInformationRepository bankInformationRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    // Test data
    private User testUser;
    private WalletType testWalletType;
    private Wallet testWallet;
    private BankInformation testBank;
    private CreateWalletRequest createWalletRequest;
    private WalletResponse walletResponse;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = User.builder()
                .userId(1)
                .userEmail("test@example.com")
                .userFullName("Test User")
                .userIsActive(true)
                .build();

        // Setup test wallet type
        testWalletType = new WalletType();
        testWalletType.setId(1L);
        testWalletType.setTypeName("Bank Account");
        testWalletType.setIsSystemDefined(true);
        
        // Setup test bank
        testBank = new BankInformation();
        testBank.setBankId(1);
        testBank.setBankCode("TEST");
        testBank.setBankLogoUrl("https://example.com/bank-logo.png");

        // Setup test wallet
        testWallet = new Wallet();
        testWallet.setId(1);
        testWallet.setWalletName("Test Wallet");
        testWallet.setCurrentBalance(new BigDecimal("1000.00"));
        testWallet.setInitialBalance(new BigDecimal("500.00"));
        testWallet.setDescription("Test wallet description");
        testWallet.setIsDefault(false);
        testWallet.setExcludeFromTotal(false);
        testWallet.setBankInformation(testBank);
        testWallet.setIconUrl("https://example.com/icon.png");
        testWallet.setIsDeleted(false);
        testWallet.setWalletCreatedAt(Instant.now());
        testWallet.setCurrencyCode("VND");
        testWallet.setUser(testUser);
        testWallet.setWalletType(testWalletType);

        // Setup create wallet request
        createWalletRequest = CreateWalletRequest.builder()
                .walletName("Test Wallet")
                .balance(new BigDecimal("1000.00"))
                .description("Test wallet description")
                .walletTypeId(1)
                .isDefault(false)
                .excludeFromTotal(false)
                .bankId(1)
                .iconUrl("https://example.com/icon.png")
                .build();

        // Setup wallet response
        walletResponse = WalletResponse.builder()
                .id(1)
                .walletName("Test Wallet")
                .currentBalance(new BigDecimal("1000.00"))
                .walletTypeName("Bank Account")
                .isDefault(false)
                .excludeFromTotal(false)
                .bankCode("TEST")
                .bankLogoUrl("https://example.com/bank-logo.png")
                .iconUrl("https://example.com/icon.png")
                .build();
    }

    // ===== CREATE WALLET TESTS =====

    @Test
    void createWallet_Success_CreatesNewWallet() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            // Mock for wallet limit validation (less than 5 wallets)
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(Arrays.asList());
            
            // Mock for wallet name uniqueness validation (name doesn't exist)
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), createWalletRequest.getWalletName())).thenReturn(false);
            
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            when(walletMapper.toEntity(createWalletRequest)).thenReturn(testWallet);
            when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.createWallet(createWalletRequest);

            // Then
            assertNotNull(result);
            assertEquals(walletResponse.getId(), result.getId());
            assertEquals(walletResponse.getWalletName(), result.getWalletName());
            assertEquals(walletResponse.getCurrentBalance(), result.getCurrentBalance());

            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(walletRepository).existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), createWalletRequest.getWalletName());
            verify(walletTypeRepository).findById(1);
            verify(walletMapper).toEntity(createWalletRequest);
            verify(walletRepository).save(any(Wallet.class));
            verify(walletMapper).toResponse(testWallet);
        }
    }

    @Test
    void createWallet_Success_CreatesDefaultWallet() {
        // Given
        createWalletRequest.setIsDefault(true);
        List<Wallet> existingWallets = Arrays.asList(testWallet);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            // Mock for wallet limit validation (less than 5 wallets)
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(existingWallets);
            
            // Mock for wallet name uniqueness validation (name doesn't exist)
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), createWalletRequest.getWalletName())).thenReturn(false);
            
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            when(walletMapper.toEntity(createWalletRequest)).thenReturn(testWallet);
            when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.createWallet(createWalletRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository, times(2)).findAllActiveByUserId(testUser.getUserId()); // Called twice: limit check + default status update
            verify(walletRepository).existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), createWalletRequest.getWalletName());
            verify(walletRepository, times(2)).save(any(Wallet.class)); // Once for existing wallets, once for new wallet
        }
    }

    @Test
    void createWallet_Success_WithZeroBalance() {
        // Given
        createWalletRequest.setBalance(BigDecimal.ZERO);
        testWallet.setCurrentBalance(BigDecimal.ZERO);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(Arrays.asList());
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), createWalletRequest.getWalletName())).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            when(walletMapper.toEntity(createWalletRequest)).thenReturn(testWallet);
            when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.createWallet(createWalletRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository).save(any(Wallet.class));
        }
    }

    @Test
    void createWallet_Success_WithLargeBalance() {
        // Given
        BigDecimal largeBalance = new BigDecimal("999999999.99");
        createWalletRequest.setBalance(largeBalance);
        testWallet.setCurrentBalance(largeBalance);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(Arrays.asList());
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), createWalletRequest.getWalletName())).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            when(walletMapper.toEntity(createWalletRequest)).thenReturn(testWallet);
            when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.createWallet(createWalletRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository).save(any(Wallet.class));
        }
    }

    @Test
    void createWallet_Success_WithNullOptionalFields() {
        // Given
        createWalletRequest.setDescription(null);
        createWalletRequest.setBankId(null);
        createWalletRequest.setIconUrl(null);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(Arrays.asList());
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), createWalletRequest.getWalletName())).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            when(walletMapper.toEntity(createWalletRequest)).thenReturn(testWallet);
            when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.createWallet(createWalletRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository).save(any(Wallet.class));
        }
    }

    @Test
    void createWallet_Success_AtWalletLimitBoundary() {
        // Given - User has exactly 4 wallets, creating 5th (still within limit)
        List<Wallet> existingWallets = Arrays.asList(
                new Wallet(), new Wallet(), new Wallet(), new Wallet()
        );

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(existingWallets);
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), createWalletRequest.getWalletName())).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            when(walletMapper.toEntity(createWalletRequest)).thenReturn(testWallet);
            when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.createWallet(createWalletRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(walletRepository).save(any(Wallet.class));
        }
    }

    @Test
    void createWallet_Success_WithLongWalletName() {
        // Given
        String longWalletName = "A".repeat(100); // Assuming this is within acceptable limits
        createWalletRequest.setWalletName(longWalletName);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(Arrays.asList());
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), longWalletName)).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            when(walletMapper.toEntity(createWalletRequest)).thenReturn(testWallet);
            when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.createWallet(createWalletRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository).existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), longWalletName);
        }
    }

    @Test
    void createWallet_Success_WithSpecialCharactersInName() {
        // Given
        String specialCharName = "My Wallet @#$%^&*()";
        createWalletRequest.setWalletName(specialCharName);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(Arrays.asList());
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), specialCharName)).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            when(walletMapper.toEntity(createWalletRequest)).thenReturn(testWallet);
            when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.createWallet(createWalletRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository).existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), specialCharName);
        }
    }

    @Test
    void createWallet_ThrowsException_WhenRequestIsNull() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);

            // When & Then
            assertThrows(
                    NullPointerException.class,
                    () -> walletService.createWallet(null)
            );
        }
    }

    @Test
    void createWallet_ThrowsException_WhenCurrentUserIsNull() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(null);

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.createWallet(createWalletRequest)
            );
            assertEquals("Unable to identify the current user", exception.getMessage());
        }
    }

    @Test
    void createWallet_ThrowsException_WhenWalletTypeNotFound() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(Arrays.asList());
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), createWalletRequest.getWalletName())).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.empty());

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.createWallet(createWalletRequest)
            );
            assertEquals("Wallet type not found with id: 1", exception.getMessage());
            verify(walletTypeRepository).findById(1);
        }
    }

    @Test
    void createWallet_ThrowsException_WhenWalletTypeIdIsZero() {
        // Given
        createWalletRequest.setWalletTypeId(0);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(Arrays.asList());
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), createWalletRequest.getWalletName())).thenReturn(false);
            when(walletTypeRepository.findById(0)).thenReturn(Optional.empty());

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.createWallet(createWalletRequest)
            );
            assertEquals("Wallet type not found with id: 0", exception.getMessage());
        }
    }

    @Test
    void createWallet_ThrowsException_WhenWalletTypeIdIsNegative() {
        // Given
        createWalletRequest.setWalletTypeId(-1);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(Arrays.asList());
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), createWalletRequest.getWalletName())).thenReturn(false);
            when(walletTypeRepository.findById(-1)).thenReturn(Optional.empty());

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.createWallet(createWalletRequest)
            );
            assertEquals("Wallet type not found with id: -1", exception.getMessage());
        }
    }

    @Test
    void createWallet_ThrowsException_WhenWalletLimitExceeded() {
        // Given - Create 5 existing wallets to reach the limit
        List<Wallet> existingWallets = Arrays.asList(
                new Wallet(), new Wallet(), new Wallet(), new Wallet(), new Wallet()
        );

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(existingWallets);

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.createWallet(createWalletRequest)
            );
            assertEquals("Maximum wallet limit reached. You can only have up to 5 wallets.", exception.getMessage());
            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
        }
    }

    @Test
    void createWallet_ThrowsException_WhenWalletNameAlreadyExists() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(Arrays.asList());
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), createWalletRequest.getWalletName())).thenReturn(true);

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.createWallet(createWalletRequest)
            );
            assertEquals("Wallet name already exists. Please choose a different name.", exception.getMessage());
            verify(walletRepository).existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), createWalletRequest.getWalletName());
        }
    }

    @Test
    void createWallet_ThrowsException_WhenWalletNameIsEmpty() {
        // Given
        createWalletRequest.setWalletName("");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(Arrays.asList());
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), "")).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            
            // Assuming mapper throws exception for empty name, or service validates this
            when(walletMapper.toEntity(createWalletRequest)).thenThrow(new IllegalArgumentException("Wallet name cannot be empty"));

            // When & Then
            assertThrows(
                    IllegalArgumentException.class,
                    () -> walletService.createWallet(createWalletRequest)
            );
        }
    }

    @Test
    void createWallet_ThrowsException_WhenWalletNameIsNull() {
        // Given
        createWalletRequest.setWalletName(null);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(Arrays.asList());
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), null)).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            
            // Assuming mapper throws exception for null name, or service validates this
            when(walletMapper.toEntity(createWalletRequest)).thenThrow(new IllegalArgumentException("Wallet name cannot be null"));

            // When & Then
            assertThrows(
                    IllegalArgumentException.class,
                    () -> walletService.createWallet(createWalletRequest)
            );
        }
    }

    @Test
    void createWallet_ThrowsException_WhenBalanceIsNegative() {
        // Given
        createWalletRequest.setBalance(new BigDecimal("-100.00"));

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(Arrays.asList());
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), createWalletRequest.getWalletName())).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            
            // Assuming mapper or service validates negative balance
            when(walletMapper.toEntity(createWalletRequest)).thenThrow(new IllegalArgumentException("Balance cannot be negative"));

            // When & Then
            assertThrows(
                    IllegalArgumentException.class,
                    () -> walletService.createWallet(createWalletRequest)
            );
        }
    }

    @Test
    void createWallet_ThrowsException_WhenBalanceIsNull() {
        // Given
        createWalletRequest.setBalance(null);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(Arrays.asList());
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), createWalletRequest.getWalletName())).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            
            // Assuming mapper or service validates null balance
            when(walletMapper.toEntity(createWalletRequest)).thenThrow(new IllegalArgumentException("Balance cannot be null"));

            // When & Then
            assertThrows(
                    IllegalArgumentException.class,
                    () -> walletService.createWallet(createWalletRequest)
            );
        }
    }

    @Test
    void createWallet_ThrowsException_WhenRepositoryThrowsException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(Arrays.asList());
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), createWalletRequest.getWalletName())).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            when(walletMapper.toEntity(createWalletRequest)).thenReturn(testWallet);
            when(walletRepository.save(any(Wallet.class))).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThrows(
                    RuntimeException.class,
                    () -> walletService.createWallet(createWalletRequest)
            );
        }
    }

    @Test
    void createWallet_ThrowsException_WhenMapperThrowsException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(Arrays.asList());
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), createWalletRequest.getWalletName())).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            when(walletMapper.toEntity(createWalletRequest)).thenThrow(new RuntimeException("Mapping error"));

            // When & Then
            assertThrows(
                    RuntimeException.class,
                    () -> walletService.createWallet(createWalletRequest)
            );
        }
    }

    @Test
    void createWallet_Success_MultipleDefaultWalletsHandledCorrectly() {
        // Given - User already has a default wallet, creating another default wallet should unset the previous one
        Wallet existingDefaultWallet = new Wallet();
        existingDefaultWallet.setId(2);
        existingDefaultWallet.setIsDefault(true);
        existingDefaultWallet.setUser(testUser);

        Wallet existingNonDefaultWallet = new Wallet();
        existingNonDefaultWallet.setId(3);
        existingNonDefaultWallet.setIsDefault(false);
        existingNonDefaultWallet.setUser(testUser);

        List<Wallet> existingWallets = Arrays.asList(existingDefaultWallet, existingNonDefaultWallet);
        createWalletRequest.setIsDefault(true);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(existingWallets);
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeleted(testUser.getUserId(), createWalletRequest.getWalletName())).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            when(walletMapper.toEntity(createWalletRequest)).thenReturn(testWallet);
            when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.createWallet(createWalletRequest);

            // Then
            assertNotNull(result);
            // Verify that the existing default wallet is updated to non-default
            assertFalse(existingDefaultWallet.getIsDefault());
            assertFalse(existingNonDefaultWallet.getIsDefault());
            
            verify(walletRepository, times(2)).findAllActiveByUserId(testUser.getUserId()); // Called twice: limit check + default status update
            verify(walletRepository, times(3)).save(any(Wallet.class)); // 2 times for existing wallets + 1 time for new wallet
        }
    }

    // ===== GET WALLET TESTS =====

    @Test
    void getWallet_Success_ReturnsWallet() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.getWallet(1);

            // Then
            assertNotNull(result);
            assertEquals(walletResponse.getId(), result.getId());
            assertEquals(walletResponse.getWalletName(), result.getWalletName());

            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletMapper).toResponse(testWallet);
        }
    }

    @Test
    void getWallet_Success_ReturnsDefaultWallet() {
        // Given
        testWallet.setIsDefault(true);
        walletResponse = WalletResponse.builder()
                .id(1)
                .walletName("Test Wallet")
                .currentBalance(new BigDecimal("1000.00"))
                .walletTypeName("Bank Account")
                .isDefault(true)
                .excludeFromTotal(false)
                .bankCode("TEST")
                .bankLogoUrl("https://example.com/bank-logo.png")
                .iconUrl("https://example.com/icon.png")
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.getWallet(1);

            // Then
            assertNotNull(result);
            assertTrue(result.getIsDefault());
            assertEquals(walletResponse.getId(), result.getId());

            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletMapper).toResponse(testWallet);
        }
    }

    @Test
    void getWallet_Success_ReturnsWalletWithZeroBalance() {
        // Given
        testWallet.setCurrentBalance(BigDecimal.ZERO);
        walletResponse = WalletResponse.builder()
                .id(1)
                .walletName("Test Wallet")
                .currentBalance(BigDecimal.ZERO)
                .walletTypeName("Bank Account")
                .isDefault(false)
                .excludeFromTotal(false)
                .bankCode("TEST")
                .bankLogoUrl("https://example.com/bank-logo.png")
                .iconUrl("https://example.com/icon.png")
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.getWallet(1);

            // Then
            assertNotNull(result);
            assertEquals(BigDecimal.ZERO, result.getCurrentBalance());

            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletMapper).toResponse(testWallet);
        }
    }

    @Test
    void getWallet_Success_ReturnsWalletWithLargeBalance() {
        // Given
        BigDecimal largeBalance = new BigDecimal("999999999.99");
        testWallet.setCurrentBalance(largeBalance);
        walletResponse = WalletResponse.builder()
                .id(1)
                .walletName("Test Wallet")
                .currentBalance(largeBalance)
                .walletTypeName("Bank Account")
                .isDefault(false)
                .excludeFromTotal(false)
                .bankCode("TEST")
                .bankLogoUrl("https://example.com/bank-logo.png")
                .iconUrl("https://example.com/icon.png")
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.getWallet(1);

            // Then
            assertNotNull(result);
            assertEquals(largeBalance, result.getCurrentBalance());

            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletMapper).toResponse(testWallet);
        }
    }

    @Test
    void getWallet_Success_ReturnsWalletWithNullOptionalFields() {
        // Given
        testWallet.setDescription(null);
        testWallet.setBankInformation(null);
        testWallet.setIconUrl(null);
        walletResponse = WalletResponse.builder()
                .id(1)
                .walletName("Test Wallet")
                .currentBalance(new BigDecimal("1000.00"))
                .walletTypeName("Bank Account")
                .isDefault(false)
                .excludeFromTotal(false)
                .bankCode(null)
                .bankLogoUrl(null)
                .iconUrl(null)
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.getWallet(1);

            // Then
            assertNotNull(result);
            assertNull(result.getBankCode());
            assertNull(result.getBankLogoUrl());

            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletMapper).toResponse(testWallet);
        }
    }

    @Test
    void getWallet_Success_ReturnsExcludedFromTotalWallet() {
        // Given
        testWallet.setExcludeFromTotal(true);
        walletResponse = WalletResponse.builder()
                .id(1)
                .walletName("Test Wallet")
                .currentBalance(new BigDecimal("1000.00"))
                .walletTypeName("Bank Account")
                .isDefault(false)
                .excludeFromTotal(true)
                .bankCode("TEST")
                .bankLogoUrl("https://example.com/bank-logo.png")
                .iconUrl("https://example.com/icon.png")
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.getWallet(1);

            // Then
            assertNotNull(result);
            assertTrue(result.getExcludeFromTotal());

            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletMapper).toResponse(testWallet);
        }
    }

    @Test
    void getWallet_ThrowsException_WhenCurrentUserIsNull() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(null);

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.getWallet(1)
            );
            assertEquals("Unable to identify the current user", exception.getMessage());
            
            // Verify repository is not called when user is null
            verify(walletRepository, never()).findByIdAndNotDeleted(any());
        }
    }

    @Test
    void getWallet_ThrowsException_WhenWalletIdIsNull() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(null)).thenReturn(Optional.empty());

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.getWallet(null)
            );
            assertEquals("Wallet not found with id: null", exception.getMessage());
            verify(walletRepository).findByIdAndNotDeleted(null);
        }
    }

    @Test
    void getWallet_ThrowsException_WhenWalletIdIsZero() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(0)).thenReturn(Optional.empty());

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.getWallet(0)
            );
            assertEquals("Wallet not found with id: 0", exception.getMessage());
            verify(walletRepository).findByIdAndNotDeleted(0);
        }
    }

    @Test
    void getWallet_ThrowsException_WhenWalletIdIsNegative() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(-1)).thenReturn(Optional.empty());

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.getWallet(-1)
            );
            assertEquals("Wallet not found with id: -1", exception.getMessage());
            verify(walletRepository).findByIdAndNotDeleted(-1);
        }
    }

    @Test
    void getWallet_ThrowsException_WhenWalletNotFound() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.empty());

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.getWallet(1)
            );
            assertEquals("Wallet not found with id: 1", exception.getMessage());
            verify(walletRepository).findByIdAndNotDeleted(1);
        }
    }

    @Test
    void getWallet_ThrowsException_WhenWalletNotFoundWithLargeId() {
        // Given
        int largeId = Integer.MAX_VALUE;
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(largeId)).thenReturn(Optional.empty());

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.getWallet(largeId)
            );
            assertEquals("Wallet not found with id: " + largeId, exception.getMessage());
            verify(walletRepository).findByIdAndNotDeleted(largeId);
        }
    }

    @Test
    void getWallet_ThrowsException_WhenUserDoesNotOwnWallet() {
        // Given
        User anotherUser = User.builder()
                .userId(2)
                .userEmail("another@example.com")
                .build();
        testWallet.setUser(anotherUser);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.getWallet(1)
            );
            assertEquals("User does not own this wallet", exception.getMessage());
            
            verify(walletRepository).findByIdAndNotDeleted(1);
            // Verify mapper is not called when ownership validation fails
            verify(walletMapper, never()).toResponse(any());
        }
    }

    @Test
    void getWallet_ThrowsException_WhenWalletOwnerIsNull() {
        // Given
        testWallet.setUser(null);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));

            // When & Then
            assertThrows(
                    NullPointerException.class,
                    () -> walletService.getWallet(1)
            );
            
            verify(walletRepository).findByIdAndNotDeleted(1);
        }
    }

    @Test
    void getWallet_ThrowsException_WhenRepositoryThrowsException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenThrow(new RuntimeException("Database connection error"));

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> walletService.getWallet(1)
            );
            assertEquals("Database connection error", exception.getMessage());
            
            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletMapper, never()).toResponse(any());
        }
    }

    @Test
    void getWallet_ThrowsException_WhenMapperThrowsException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletMapper.toResponse(testWallet)).thenThrow(new RuntimeException("Mapping error"));

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> walletService.getWallet(1)
            );
            assertEquals("Mapping error", exception.getMessage());
            
            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletMapper).toResponse(testWallet);
        }
    }

    @Test
    void getWallet_Success_WithDifferentWalletTypes() {
        // Given
        WalletType creditCardType = new WalletType();
        creditCardType.setId(2L);
        creditCardType.setTypeName("Credit Card");
        creditCardType.setIsSystemDefined(true);
        
        testWallet.setWalletType(creditCardType);
        walletResponse = WalletResponse.builder()
                .id(1)
                .walletName("Test Wallet")
                .currentBalance(new BigDecimal("1000.00"))
                .walletTypeName("Credit Card")
                .isDefault(false)
                .excludeFromTotal(false)
                .bankCode("TEST")
                .bankLogoUrl("https://example.com/bank-logo.png")
                .iconUrl("https://example.com/icon.png")
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.getWallet(1);

            // Then
            assertNotNull(result);
            assertEquals("Credit Card", result.getWalletTypeName());

            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletMapper).toResponse(testWallet);
        }
    }

    @Test
    void getWallet_Success_VerifyAllFieldsReturned() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.getWallet(1);

            // Then
            assertNotNull(result);
            assertNotNull(result.getId());
            assertNotNull(result.getWalletName());
            assertNotNull(result.getCurrentBalance());
            assertNotNull(result.getWalletTypeName());
            assertNotNull(result.getIsDefault());
            assertNotNull(result.getExcludeFromTotal());
            // Optional fields can be null
            // assertNotNull(result.getBankCode());
            // assertNotNull(result.getBankLogoUrl());

            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletMapper).toResponse(testWallet);
        }
    }

    // ===== GET ALL WALLETS TESTS =====

    @Test
    void getAllWallets_Success_ReturnsWalletList() {
        // Given
        List<Wallet> wallets = Arrays.asList(testWallet);
        List<WalletResponse> walletResponses = Arrays.asList(walletResponse);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(wallets);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            List<WalletResponse> result = walletService.getAllWallets();

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(walletResponse.getId(), result.get(0).getId());

            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(walletMapper).toResponse(testWallet);
        }
    }

    @Test
    void getAllWallets_Success_ReturnsEmptyList() {
        // Given
        List<Wallet> emptyWallets = Arrays.asList();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(emptyWallets);

            // When
            List<WalletResponse> result = walletService.getAllWallets();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
            assertEquals(0, result.size());

            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(walletMapper, never()).toResponse(any());
        }
    }

    @Test
    void getAllWallets_Success_ReturnsMultipleWallets() {
        // Given
        Wallet wallet2 = new Wallet();
        wallet2.setId(2);
        wallet2.setWalletName("Wallet 2");
        wallet2.setCurrentBalance(new BigDecimal("2000.00"));
        wallet2.setUser(testUser);
        wallet2.setWalletType(testWalletType);

        Wallet wallet3 = new Wallet();
        wallet3.setId(3);
        wallet3.setWalletName("Wallet 3");
        wallet3.setCurrentBalance(new BigDecimal("3000.00"));
        wallet3.setUser(testUser);
        wallet3.setWalletType(testWalletType);

        List<Wallet> wallets = Arrays.asList(testWallet, wallet2, wallet3);

        WalletResponse response2 = WalletResponse.builder()
                .id(2)
                .walletName("Wallet 2")
                .currentBalance(new BigDecimal("2000.00"))
                .build();

        WalletResponse response3 = WalletResponse.builder()
                .id(3)
                .walletName("Wallet 3")
                .currentBalance(new BigDecimal("3000.00"))
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(wallets);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);
            when(walletMapper.toResponse(wallet2)).thenReturn(response2);
            when(walletMapper.toResponse(wallet3)).thenReturn(response3);

            // When
            List<WalletResponse> result = walletService.getAllWallets();

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals(1, result.get(0).getId());
            assertEquals(2, result.get(1).getId());
            assertEquals(3, result.get(2).getId());

            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(walletMapper, times(3)).toResponse(any(Wallet.class));
        }
    }

    @Test
    void getAllWallets_Success_ReturnsMaximumWallets() {
        // Given - User has 5 wallets (maximum allowed)
        List<Wallet> wallets = Arrays.asList(
                new Wallet(), new Wallet(), new Wallet(), new Wallet(), new Wallet()
        );
        wallets.forEach(w -> w.setUser(testUser));

        List<WalletResponse> responses = Arrays.asList(
                WalletResponse.builder().id(1).build(),
                WalletResponse.builder().id(2).build(),
                WalletResponse.builder().id(3).build(),
                WalletResponse.builder().id(4).build(),
                WalletResponse.builder().id(5).build()
        );

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(wallets);
            when(walletMapper.toResponse(any(Wallet.class)))
                    .thenReturn(responses.get(0), responses.get(1), responses.get(2), responses.get(3), responses.get(4));

            // When
            List<WalletResponse> result = walletService.getAllWallets();

            // Then
            assertNotNull(result);
            assertEquals(5, result.size());

            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(walletMapper, times(5)).toResponse(any(Wallet.class));
        }
    }

    @Test
    void getAllWallets_Success_ReturnsMixedWalletTypes() {
        // Given
        WalletType creditCardType = new WalletType();
        creditCardType.setId(2L);
        creditCardType.setTypeName("Credit Card");

        WalletType cashType = new WalletType();
        cashType.setId(3L);
        cashType.setTypeName("Cash");

        Wallet bankWallet = testWallet; // Bank Account
        Wallet creditWallet = new Wallet();
        creditWallet.setId(2);
        creditWallet.setWalletType(creditCardType);
        creditWallet.setUser(testUser);

        Wallet cashWallet = new Wallet();
        cashWallet.setId(3);
        cashWallet.setWalletType(cashType);
        cashWallet.setUser(testUser);

        List<Wallet> wallets = Arrays.asList(bankWallet, creditWallet, cashWallet);

        WalletResponse bankResponse = walletResponse;
        WalletResponse creditResponse = WalletResponse.builder()
                .id(2)
                .walletTypeName("Credit Card")
                .build();
        WalletResponse cashResponse = WalletResponse.builder()
                .id(3)
                .walletTypeName("Cash")
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(wallets);
            when(walletMapper.toResponse(bankWallet)).thenReturn(bankResponse);
            when(walletMapper.toResponse(creditWallet)).thenReturn(creditResponse);
            when(walletMapper.toResponse(cashWallet)).thenReturn(cashResponse);

            // When
            List<WalletResponse> result = walletService.getAllWallets();

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals("Bank Account", result.get(0).getWalletTypeName());
            assertEquals("Credit Card", result.get(1).getWalletTypeName());
            assertEquals("Cash", result.get(2).getWalletTypeName());

            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(walletMapper, times(3)).toResponse(any(Wallet.class));
        }
    }

    @Test
    void getAllWallets_Success_ReturnsMixedWalletStates() {
        // Given
        Wallet defaultWallet = new Wallet();
        defaultWallet.setId(1);
        defaultWallet.setIsDefault(true);
        defaultWallet.setExcludeFromTotal(false);
        defaultWallet.setUser(testUser);

        Wallet excludedWallet = new Wallet();
        excludedWallet.setId(2);
        excludedWallet.setIsDefault(false);
        excludedWallet.setExcludeFromTotal(true);
        excludedWallet.setUser(testUser);

        Wallet normalWallet = new Wallet();
        normalWallet.setId(3);
        normalWallet.setIsDefault(false);
        normalWallet.setExcludeFromTotal(false);
        normalWallet.setUser(testUser);

        List<Wallet> wallets = Arrays.asList(defaultWallet, excludedWallet, normalWallet);

        WalletResponse defaultResponse = WalletResponse.builder()
                .id(1)
                .isDefault(true)
                .excludeFromTotal(false)
                .build();
        WalletResponse excludedResponse = WalletResponse.builder()
                .id(2)
                .isDefault(false)
                .excludeFromTotal(true)
                .build();
        WalletResponse normalResponse = WalletResponse.builder()
                .id(3)
                .isDefault(false)
                .excludeFromTotal(false)
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(wallets);
            when(walletMapper.toResponse(defaultWallet)).thenReturn(defaultResponse);
            when(walletMapper.toResponse(excludedWallet)).thenReturn(excludedResponse);
            when(walletMapper.toResponse(normalWallet)).thenReturn(normalResponse);

            // When
            List<WalletResponse> result = walletService.getAllWallets();

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());
            assertTrue(result.get(0).getIsDefault());
            assertTrue(result.get(1).getExcludeFromTotal());
            assertFalse(result.get(2).getIsDefault());
            assertFalse(result.get(2).getExcludeFromTotal());

            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(walletMapper, times(3)).toResponse(any(Wallet.class));
        }
    }

    @Test
    void getAllWallets_Success_ReturnsWalletsWithVariousBalances() {
        // Given
        Wallet zeroBalanceWallet = new Wallet();
        zeroBalanceWallet.setId(1);
        zeroBalanceWallet.setCurrentBalance(BigDecimal.ZERO);
        zeroBalanceWallet.setUser(testUser);

        Wallet largeBalanceWallet = new Wallet();
        largeBalanceWallet.setId(2);
        largeBalanceWallet.setCurrentBalance(new BigDecimal("999999999.99"));
        largeBalanceWallet.setUser(testUser);

        Wallet smallBalanceWallet = new Wallet();
        smallBalanceWallet.setId(3);
        smallBalanceWallet.setCurrentBalance(new BigDecimal("0.01"));
        smallBalanceWallet.setUser(testUser);

        List<Wallet> wallets = Arrays.asList(zeroBalanceWallet, largeBalanceWallet, smallBalanceWallet);

        WalletResponse zeroResponse = WalletResponse.builder()
                .id(1)
                .currentBalance(BigDecimal.ZERO)
                .build();
        WalletResponse largeResponse = WalletResponse.builder()
                .id(2)
                .currentBalance(new BigDecimal("999999999.99"))
                .build();
        WalletResponse smallResponse = WalletResponse.builder()
                .id(3)
                .currentBalance(new BigDecimal("0.01"))
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(wallets);
            when(walletMapper.toResponse(zeroBalanceWallet)).thenReturn(zeroResponse);
            when(walletMapper.toResponse(largeBalanceWallet)).thenReturn(largeResponse);
            when(walletMapper.toResponse(smallBalanceWallet)).thenReturn(smallResponse);

            // When
            List<WalletResponse> result = walletService.getAllWallets();

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals(BigDecimal.ZERO, result.get(0).getCurrentBalance());
            assertEquals(new BigDecimal("999999999.99"), result.get(1).getCurrentBalance());
            assertEquals(new BigDecimal("0.01"), result.get(2).getCurrentBalance());

            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(walletMapper, times(3)).toResponse(any(Wallet.class));
        }
    }

    @Test
    void getAllWallets_Success_ReturnsWalletsWithNullOptionalFields() {
        // Given
        Wallet walletWithNulls = new Wallet();
        walletWithNulls.setId(1);
        walletWithNulls.setWalletName("Test Wallet");
        walletWithNulls.setCurrentBalance(new BigDecimal("1000.00"));
        walletWithNulls.setDescription(null);
        walletWithNulls.setBankInformation(null);
        walletWithNulls.setIconUrl(null);
        walletWithNulls.setUser(testUser);

        List<Wallet> wallets = Arrays.asList(walletWithNulls);

        WalletResponse responseWithNulls = WalletResponse.builder()
                .id(1)
                .walletName("Test Wallet")
                .currentBalance(new BigDecimal("1000.00"))
                .bankCode(null)
                .bankLogoUrl(null)
                .iconUrl(null)
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(wallets);
            when(walletMapper.toResponse(walletWithNulls)).thenReturn(responseWithNulls);

            // When
            List<WalletResponse> result = walletService.getAllWallets();

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertNull(result.get(0).getBankCode());
            assertNull(result.get(0).getBankLogoUrl());

            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(walletMapper).toResponse(walletWithNulls);
        }
    }

    @Test
    void getAllWallets_Success_VerifyCorrectMapping() {
        // Given
        List<Wallet> wallets = Arrays.asList(testWallet);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(wallets);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            List<WalletResponse> result = walletService.getAllWallets();

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            WalletResponse response = result.get(0);
            assertEquals(walletResponse.getId(), response.getId());
            assertEquals(walletResponse.getWalletName(), response.getWalletName());
            assertEquals(walletResponse.getCurrentBalance(), response.getCurrentBalance());
            assertEquals(walletResponse.getWalletTypeName(), response.getWalletTypeName());
            assertEquals(walletResponse.getIsDefault(), response.getIsDefault());
            assertEquals(walletResponse.getExcludeFromTotal(), response.getExcludeFromTotal());

            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(walletMapper).toResponse(testWallet);
        }
    }

    @Test
    void getAllWallets_ThrowsException_WhenCurrentUserIsNull() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(null);

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.getAllWallets()
            );
            assertEquals("Unable to identify the current user", exception.getMessage());
            
            // Verify repository is not called when user is null
            verify(walletRepository, never()).findAllActiveByUserId(any());
            verify(walletMapper, never()).toResponse(any());
        }
    }

    @Test
    void getAllWallets_ThrowsException_WhenRepositoryThrowsException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId()))
                    .thenThrow(new RuntimeException("Database connection error"));

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> walletService.getAllWallets()
            );
            assertEquals("Database connection error", exception.getMessage());
            
            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(walletMapper, never()).toResponse(any());
        }
    }

    @Test
    void getAllWallets_ThrowsException_WhenMapperThrowsException() {
        // Given
        List<Wallet> wallets = Arrays.asList(testWallet);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(wallets);
            when(walletMapper.toResponse(testWallet)).thenThrow(new RuntimeException("Mapping error"));

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> walletService.getAllWallets()
            );
            assertEquals("Mapping error", exception.getMessage());
            
            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(walletMapper).toResponse(testWallet);
        }
    }

    @Test
    void getAllWallets_ThrowsException_WhenMapperThrowsExceptionForSecondWallet() {
        // Given
        Wallet wallet2 = new Wallet();
        wallet2.setId(2);
        wallet2.setUser(testUser);

        List<Wallet> wallets = Arrays.asList(testWallet, wallet2);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(wallets);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);
            when(walletMapper.toResponse(wallet2)).thenThrow(new RuntimeException("Mapping error for second wallet"));

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> walletService.getAllWallets()
            );
            assertEquals("Mapping error for second wallet", exception.getMessage());
            
            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(walletMapper).toResponse(testWallet);
            verify(walletMapper).toResponse(wallet2);
        }
    }

    @Test
    void getAllWallets_Success_WithUserHavingDifferentUserId() {
        // Given
        User anotherUser = User.builder()
                .userId(999)
                .userEmail("another@example.com")
                .build();

        List<Wallet> emptyWallets = Arrays.asList();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(anotherUser);
            when(walletRepository.findAllActiveByUserId(999)).thenReturn(emptyWallets);

            // When
            List<WalletResponse> result = walletService.getAllWallets();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(walletRepository).findAllActiveByUserId(999);
            verify(walletMapper, never()).toResponse(any());
        }
    }

    // ===== UPDATE WALLET TESTS =====

    @Test
    void updateWallet_Success_UpdatesWallet() {
        // Given
        CreateWalletRequest updateRequest = CreateWalletRequest.builder()
                .walletName("Updated Wallet")
                .balance(new BigDecimal("2000.00"))
                .walletTypeId(1)
                .isDefault(false)
                .currencyCode("VND")
                .build();

        BigDecimal income = new BigDecimal("300.00");
        BigDecimal expense = new BigDecimal("100.00");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            
            // Mock for wallet name uniqueness validation (name doesn't exist for other wallets)
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), updateRequest.getWalletName(), 1)).thenReturn(false);
            
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            
            // Mock transaction repository calls for balance calculation
            when(transactionRepository.sumIncomeWallet(1, testUser.getUserId())).thenReturn(income);
            when(transactionRepository.sumExpenseWallet(1, testUser.getUserId())).thenReturn(expense);
            
            when(walletRepository.save(testWallet)).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.updateWallet(1, updateRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletRepository).existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), updateRequest.getWalletName(), 1);
            verify(walletTypeRepository).findById(1);
            verify(transactionRepository).sumIncomeWallet(1, testUser.getUserId());
            verify(transactionRepository).sumExpenseWallet(1, testUser.getUserId());
//            verify(walletMapper).updateEntity(testWallet, updateRequest);
            verify(walletRepository).save(testWallet);
            verify(walletMapper).toResponse(testWallet);
            
            // Verify balance calculation: initialBalance (500) + income (300) - expense (100) = 700
            assertEquals(new BigDecimal("700.00"), testWallet.getCurrentBalance());
            assertEquals("VND", testWallet.getCurrencyCode());
        }
    }

    @Test
    void updateWallet_Success_UpdatesToDefaultWallet() {
        // Given
        CreateWalletRequest updateRequest = CreateWalletRequest.builder()
                .walletName("Updated Wallet")
                .balance(new BigDecimal("2000.00"))
                .walletTypeId(1)
                .isDefault(true)
                .currencyCode("VND")
                .build();

        List<Wallet> existingWallets = Arrays.asList(testWallet);
        BigDecimal income = new BigDecimal("200.00");
        BigDecimal expense = new BigDecimal("50.00");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            
            // Mock for wallet name uniqueness validation (name doesn't exist for other wallets)
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), updateRequest.getWalletName(), 1)).thenReturn(false);
            
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(existingWallets);
            
            // Mock transaction repository calls for balance calculation
            when(transactionRepository.sumIncomeWallet(1, testUser.getUserId())).thenReturn(income);
            when(transactionRepository.sumExpenseWallet(1, testUser.getUserId())).thenReturn(expense);
            
            when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.updateWallet(1, updateRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository).existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), updateRequest.getWalletName(), 1);
            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(transactionRepository).sumIncomeWallet(1, testUser.getUserId());
            verify(transactionRepository).sumExpenseWallet(1, testUser.getUserId());
            verify(walletRepository, times(2)).save(any(Wallet.class)); // Once for existing wallets, once for updated wallet
            
            // Verify balance calculation: initialBalance (500) + income (200) - expense (50) = 650
            assertEquals(new BigDecimal("650.00"), testWallet.getCurrentBalance());
        }
    }

    @Test
    void updateWallet_Success_UpdatesFromDefaultToNonDefault() {
        // Given
        testWallet.setIsDefault(true);
        CreateWalletRequest updateRequest = CreateWalletRequest.builder()
                .walletName("Updated Wallet")
                .balance(new BigDecimal("2000.00"))
                .walletTypeId(1)
                .isDefault(false)
                .currencyCode("USD")
                .build();

        BigDecimal income = new BigDecimal("150.00");
        BigDecimal expense = new BigDecimal("75.00");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), updateRequest.getWalletName(), 1)).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            
            // Mock transaction repository calls for balance calculation
            when(transactionRepository.sumIncomeWallet(1, testUser.getUserId())).thenReturn(income);
            when(transactionRepository.sumExpenseWallet(1, testUser.getUserId())).thenReturn(expense);
            
            when(walletRepository.save(testWallet)).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.updateWallet(1, updateRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository).save(testWallet);
//            verify(walletMapper).updateEntity(testWallet, updateRequest);
            verify(transactionRepository).sumIncomeWallet(1, testUser.getUserId());
            verify(transactionRepository).sumExpenseWallet(1, testUser.getUserId());
            
            // Verify balance calculation: initialBalance (500) + income (150) - expense (75) = 575
            assertEquals(new BigDecimal("575.00"), testWallet.getCurrentBalance());
            assertEquals("USD", testWallet.getCurrencyCode());
        }
    }

    @Test
    void updateWallet_Success_UpdatesWalletType() {
        // Given
        WalletType newWalletType = new WalletType();
        newWalletType.setId(2L);
        newWalletType.setTypeName("Credit Card");

        CreateWalletRequest updateRequest = CreateWalletRequest.builder()
                .walletName("Updated Wallet")
                .balance(new BigDecimal("2000.00"))
                .walletTypeId(2)
                .isDefault(false)
                .currencyCode("EUR")
                .build();

        BigDecimal income = new BigDecimal("400.00");
        BigDecimal expense = new BigDecimal("200.00");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), updateRequest.getWalletName(), 1)).thenReturn(false);
            when(walletTypeRepository.findById(2)).thenReturn(Optional.of(newWalletType));
            
            // Mock transaction repository calls for balance calculation
            when(transactionRepository.sumIncomeWallet(1, testUser.getUserId())).thenReturn(income);
            when(transactionRepository.sumExpenseWallet(1, testUser.getUserId())).thenReturn(expense);
            
            when(walletRepository.save(testWallet)).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.updateWallet(1, updateRequest);

            // Then
            assertNotNull(result);
            verify(walletTypeRepository).findById(2);
            verify(transactionRepository).sumIncomeWallet(1, testUser.getUserId());
            verify(transactionRepository).sumExpenseWallet(1, testUser.getUserId());
            assertEquals(newWalletType, testWallet.getWalletType());
            
            // Verify balance calculation: initialBalance (500) + income (400) - expense (200) = 700
            assertEquals(new BigDecimal("700.00"), testWallet.getCurrentBalance());
            assertEquals("EUR", testWallet.getCurrencyCode());
        }
    }

    @Test
    void updateWallet_Success_UpdatesWithZeroBalance() {
        // Given
        CreateWalletRequest updateRequest = CreateWalletRequest.builder()
                .walletName("Updated Wallet")
                .balance(BigDecimal.ZERO)
                .walletTypeId(1)
                .isDefault(false)
                .currencyCode("VND")
                .build();

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), updateRequest.getWalletName(), 1)).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            
            // Mock transaction repository calls for balance calculation
            when(transactionRepository.sumIncomeWallet(1, testUser.getUserId())).thenReturn(income);
            when(transactionRepository.sumExpenseWallet(1, testUser.getUserId())).thenReturn(expense);
            
            when(walletRepository.save(testWallet)).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.updateWallet(1, updateRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository).save(testWallet);
            verify(transactionRepository).sumIncomeWallet(1, testUser.getUserId());
            verify(transactionRepository).sumExpenseWallet(1, testUser.getUserId());
            
            // Verify balance calculation: initialBalance (500) + income (0) - expense (0) = 500
            assertEquals(new BigDecimal("500.00"), testWallet.getCurrentBalance());
        }
    }

    @Test
    void updateWallet_Success_UpdatesWithLargeBalance() {
        // Given
        BigDecimal largeBalance = new BigDecimal("999999999.99");
        CreateWalletRequest updateRequest = CreateWalletRequest.builder()
                .walletName("Updated Wallet")
                .balance(largeBalance)
                .walletTypeId(1)
                .isDefault(false)
                .currencyCode("JPY")
                .build();

        BigDecimal income = new BigDecimal("100000.00");
        BigDecimal expense = new BigDecimal("50000.00");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), updateRequest.getWalletName(), 1)).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            
            // Mock transaction repository calls for balance calculation
            when(transactionRepository.sumIncomeWallet(1, testUser.getUserId())).thenReturn(income);
            when(transactionRepository.sumExpenseWallet(1, testUser.getUserId())).thenReturn(expense);
            
            when(walletRepository.save(testWallet)).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.updateWallet(1, updateRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository).save(testWallet);
            verify(transactionRepository).sumIncomeWallet(1, testUser.getUserId());
            verify(transactionRepository).sumExpenseWallet(1, testUser.getUserId());
            
            // Verify balance calculation: initialBalance (500) + income (100000) - expense (50000) = 50500
            assertEquals(new BigDecimal("50500.00"), testWallet.getCurrentBalance());
            assertEquals("JPY", testWallet.getCurrencyCode());
        }
    }

    @Test
    void updateWallet_Success_UpdatesWithNullOptionalFields() {
        // Given
        CreateWalletRequest updateRequest = CreateWalletRequest.builder()
                .walletName("Updated Wallet")
                .balance(new BigDecimal("2000.00"))
                .walletTypeId(1)
                .isDefault(false)
                .description(null)
                .bankId(null)
                .iconUrl(null)
                .currencyCode(null) // Null currency code should not update existing currency
                .build();

        BigDecimal income = new BigDecimal("250.00");
        BigDecimal expense = new BigDecimal("100.00");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), updateRequest.getWalletName(), 1)).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            
            // Mock transaction repository calls for balance calculation
            when(transactionRepository.sumIncomeWallet(1, testUser.getUserId())).thenReturn(income);
            when(transactionRepository.sumExpenseWallet(1, testUser.getUserId())).thenReturn(expense);
            
            when(walletRepository.save(testWallet)).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.updateWallet(1, updateRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository).save(testWallet);
            verify(transactionRepository).sumIncomeWallet(1, testUser.getUserId());
            verify(transactionRepository).sumExpenseWallet(1, testUser.getUserId());
            
            // Verify balance calculation: initialBalance (500) + income (250) - expense (100) = 650
            assertEquals(new BigDecimal("650.00"), testWallet.getCurrentBalance());
            // Currency code should remain unchanged (VND from setup)
            assertEquals("VND", testWallet.getCurrencyCode());
        }
    }

    @Test
    void updateWallet_Success_UpdatesWithSpecialCharactersInName() {
        // Given
        String specialCharName = "Updated Wallet @#$%^&*()";
        CreateWalletRequest updateRequest = CreateWalletRequest.builder()
                .walletName(specialCharName)
                .balance(new BigDecimal("2000.00"))
                .walletTypeId(1)
                .isDefault(false)
                .currencyCode("GBP")
                .build();

        BigDecimal income = new BigDecimal("80.00");
        BigDecimal expense = new BigDecimal("30.00");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), specialCharName, 1)).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            
            // Mock transaction repository calls for balance calculation
            when(transactionRepository.sumIncomeWallet(1, testUser.getUserId())).thenReturn(income);
            when(transactionRepository.sumExpenseWallet(1, testUser.getUserId())).thenReturn(expense);
            
            when(walletRepository.save(testWallet)).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.updateWallet(1, updateRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository).existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), specialCharName, 1);
            verify(transactionRepository).sumIncomeWallet(1, testUser.getUserId());
            verify(transactionRepository).sumExpenseWallet(1, testUser.getUserId());
            
            // Verify balance calculation: initialBalance (500) + income (80) - expense (30) = 550
            assertEquals(new BigDecimal("550.00"), testWallet.getCurrentBalance());
            assertEquals("GBP", testWallet.getCurrencyCode());
        }
    }

    @Test
    void updateWallet_Success_KeepsSameWalletName() {
        // Given - Update with same wallet name should be allowed
        CreateWalletRequest updateRequest = CreateWalletRequest.builder()
                .walletName(testWallet.getWalletName()) // Same name
                .balance(new BigDecimal("2000.00"))
                .walletTypeId(1)
                .isDefault(false)
                .currencyCode("CAD")
                .build();

        BigDecimal income = new BigDecimal("120.00");
        BigDecimal expense = new BigDecimal("60.00");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), updateRequest.getWalletName(), 1)).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            
            // Mock transaction repository calls for balance calculation
            when(transactionRepository.sumIncomeWallet(1, testUser.getUserId())).thenReturn(income);
            when(transactionRepository.sumExpenseWallet(1, testUser.getUserId())).thenReturn(expense);
            
            when(walletRepository.save(testWallet)).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.updateWallet(1, updateRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository).save(testWallet);
            verify(transactionRepository).sumIncomeWallet(1, testUser.getUserId());
            verify(transactionRepository).sumExpenseWallet(1, testUser.getUserId());
            
            // Verify balance calculation: initialBalance (500) + income (120) - expense (60) = 560
            assertEquals(new BigDecimal("560.00"), testWallet.getCurrentBalance());
            assertEquals("CAD", testWallet.getCurrencyCode());
        }
    }

    @Test
    void updateWallet_ThrowsException_WhenCurrentUserIsNull() {
        // Given
        CreateWalletRequest updateRequest = CreateWalletRequest.builder()
                .walletName("Updated Wallet")
                .balance(new BigDecimal("2000.00"))
                .walletTypeId(1)
                .isDefault(false)
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(null);

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.updateWallet(1, updateRequest)
            );
            assertEquals("Unable to identify the current user", exception.getMessage());
            
            verify(walletRepository, never()).findByIdAndNotDeleted(any());
        }
    }

    @Test
    void updateWallet_ThrowsException_WhenRequestIsNull() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));

            // When & Then - The service should throw NullPointerException when trying to access request.getWalletName()
            assertThrows(
                    NullPointerException.class,
                    () -> walletService.updateWallet(1, null)
            );
            
            verify(walletRepository).findByIdAndNotDeleted(1);
        }
    }

    @Test
    void updateWallet_ThrowsException_WhenWalletIdIsNull() {
        // Given
        CreateWalletRequest updateRequest = CreateWalletRequest.builder()
                .walletName("Updated Wallet")
                .balance(new BigDecimal("2000.00"))
                .walletTypeId(1)
                .isDefault(false)
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(null)).thenReturn(Optional.empty());

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.updateWallet(null, updateRequest)
            );
            assertEquals("Wallet not found with id: null", exception.getMessage());
        }
    }

    @Test
    void updateWallet_ThrowsException_WhenWalletIdIsZero() {
        // Given
        CreateWalletRequest updateRequest = CreateWalletRequest.builder()
                .walletName("Updated Wallet")
                .balance(new BigDecimal("2000.00"))
                .walletTypeId(1)
                .isDefault(false)
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(0)).thenReturn(Optional.empty());

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.updateWallet(0, updateRequest)
            );
            assertEquals("Wallet not found with id: 0", exception.getMessage());
        }
    }

    @Test
    void updateWallet_ThrowsException_WhenWalletIdIsNegative() {
        // Given
        CreateWalletRequest updateRequest = CreateWalletRequest.builder()
                .walletName("Updated Wallet")
                .balance(new BigDecimal("2000.00"))
                .walletTypeId(1)
                .isDefault(false)
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(-1)).thenReturn(Optional.empty());

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.updateWallet(-1, updateRequest)
            );
            assertEquals("Wallet not found with id: -1", exception.getMessage());
        }
    }

    @Test
    void updateWallet_ThrowsException_WhenWalletNotFound() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.empty());

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.updateWallet(1, createWalletRequest)
            );
            assertEquals("Wallet not found with id: 1", exception.getMessage());
        }
    }

    @Test
    void updateWallet_ThrowsException_WhenUserDoesNotOwnWallet() {
        // Given
        User anotherUser = User.builder()
                .userId(2)
                .userEmail("another@example.com")
                .build();
        testWallet.setUser(anotherUser);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.updateWallet(1, createWalletRequest)
            );
            assertEquals("User does not own this wallet", exception.getMessage());
        }
    }

    @Test
    void updateWallet_ThrowsException_WhenWalletTypeNotFound() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), createWalletRequest.getWalletName(), 1)).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.empty());

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.updateWallet(1, createWalletRequest)
            );
            assertEquals("Wallet type not found with id: 1", exception.getMessage());
        }
    }

    @Test
    void updateWallet_ThrowsException_WhenWalletNameAlreadyExists() {
        // Given
        CreateWalletRequest updateRequest = CreateWalletRequest.builder()
                .walletName("Existing Wallet Name")
                .balance(new BigDecimal("2000.00"))
                .walletTypeId(1)
                .isDefault(false)
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), updateRequest.getWalletName(), 1)).thenReturn(true);

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.updateWallet(1, updateRequest)
            );
            assertEquals("Wallet name already exists. Please choose a different name.", exception.getMessage());
            verify(walletRepository).existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), updateRequest.getWalletName(), 1);
        }
    }

    @Test
    void updateWallet_AllowsEmptyWalletName_WhenNoValidation() {
        // Given
        CreateWalletRequest updateRequest = CreateWalletRequest.builder()
                .walletName("")
                .balance(new BigDecimal("2000.00"))
                .walletTypeId(1)
                .isDefault(false)
                .build();

        BigDecimal income = new BigDecimal("100.00");
        BigDecimal expense = new BigDecimal("50.00");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);

            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), "", 1))
                    .thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));

            when(transactionRepository.sumIncomeWallet(1, testUser.getUserId())).thenReturn(income);
            when(transactionRepository.sumExpenseWallet(1, testUser.getUserId())).thenReturn(expense);

            when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(walletMapper.toResponse(any(Wallet.class))).thenReturn(new WalletResponse());

            // When
            WalletResponse result = walletService.updateWallet(1, updateRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository).save(any(Wallet.class));
        }
    }


    @Test
    void updateWallet_ShouldUpdateEvenWhenBalanceIsNegative() {
        // Given
        CreateWalletRequest updateRequest = CreateWalletRequest.builder()
                .walletName("Updated Wallet")
                .balance(new BigDecimal("-100.00")) // balance âm, nhưng service không validate
                .walletTypeId(1)
                .isDefault(false)
                .build();

        BigDecimal income = new BigDecimal("100.00");
        BigDecimal expense = new BigDecimal("50.00");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);

            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeletedAndIdNot(
                    testUser.getUserId(), updateRequest.getWalletName(), 1)).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));

            when(transactionRepository.sumIncomeWallet(1, testUser.getUserId())).thenReturn(income);
            when(transactionRepository.sumExpenseWallet(1, testUser.getUserId())).thenReturn(expense);

            when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(walletMapper.toResponse(any(Wallet.class))).thenReturn(new WalletResponse());

            // When
            WalletResponse result = walletService.updateWallet(1, updateRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository).save(any(Wallet.class));
        }
    }


    @Test
    void updateWallet_ThrowsException_WhenRepositoryThrowsException() {
        // Given
        CreateWalletRequest updateRequest = CreateWalletRequest.builder()
                .walletName("Updated Wallet")
                .balance(new BigDecimal("2000.00"))
                .walletTypeId(1)
                .isDefault(false)
                .build();

        BigDecimal income = new BigDecimal("100.00");
        BigDecimal expense = new BigDecimal("50.00");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), updateRequest.getWalletName(), 1)).thenReturn(false);
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            
            // Mock transaction repository calls for balance calculation
            when(transactionRepository.sumIncomeWallet(1, testUser.getUserId())).thenReturn(income);
            when(transactionRepository.sumExpenseWallet(1, testUser.getUserId())).thenReturn(expense);
            
            when(walletRepository.save(testWallet)).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThrows(
                    RuntimeException.class,
                    () -> walletService.updateWallet(1, updateRequest)
            );
        }
    }

    @Test
    void updateWallet_ThrowsException_WhenMapperThrowsException() {
        // Given
        CreateWalletRequest updateRequest = CreateWalletRequest.builder()
                .walletName("Updated Wallet")
                .balance(new BigDecimal("2000.00"))
                .walletTypeId(1)
                .isDefault(false)
                .build();

        BigDecimal income = new BigDecimal("100.00");
        BigDecimal expense = new BigDecimal("50.00");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);

            when(walletRepository.findByIdAndNotDeleted(1))
                    .thenReturn(Optional.of(testWallet));
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeletedAndIdNot(
                    testUser.getUserId(), updateRequest.getWalletName(), 1))
                    .thenReturn(false);
            when(walletTypeRepository.findById(1))
                    .thenReturn(Optional.of(testWalletType));

            when(transactionRepository.sumIncomeWallet(1, testUser.getUserId()))
                    .thenReturn(income);
            when(transactionRepository.sumExpenseWallet(1, testUser.getUserId()))
                    .thenReturn(expense);

            // Mock mapper để ném RuntimeException khi toResponse được gọi
            when(walletMapper.toResponse(any(Wallet.class)))
                    .thenThrow(new RuntimeException("Mapping error"));

            // When & Then
            assertThrows(
                    RuntimeException.class,
                    () -> walletService.updateWallet(1, updateRequest)
            );
        }
    }



    // ===== DELETE WALLET TESTS =====

    @Test
    void deleteWallet_Success_SoftDeletesWallet() {
        try (MockedStatic<UserUtils> mocked = mockStatic(UserUtils.class)) {
            mocked.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            
            // Mock multiple active wallets so the "last wallet" validation doesn't trigger
            List<Wallet> multipleWallets = Arrays.asList(testWallet, new Wallet());
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(multipleWallets);
            
            // Mock other dependencies
            doNothing().when(budgetWalletRepository).deleteBudgetWalletByWallet(1);
            when(transactionRepository.listTransactionByAllWallet(1, testUser.getUserId()))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.saveAll(Collections.emptyList()))
                    .thenReturn(Collections.emptyList());

            walletService.deleteWallet(1);

            assertTrue(testWallet.getIsDeleted(), "Wallet should be marked as deleted");
            assertNotNull(testWallet.getDeletedAt(), "DeletedAt should be set");

            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(budgetWalletRepository).deleteBudgetWalletByWallet(1);
            verify(transactionRepository).listTransactionByAllWallet(1, testUser.getUserId());
            verify(transactionRepository).saveAll(Collections.emptyList());
            verify(walletRepository).save(testWallet);
        }
    }

    @Test
    void deleteWallet_Success_DeletesDefaultWallet() {
        testWallet.setIsDefault(true);

        try (MockedStatic<UserUtils> mocked = mockStatic(UserUtils.class)) {
            mocked.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            
            // Mock multiple active wallets so the "last wallet" validation doesn't trigger
            Wallet otherWallet = new Wallet();
            otherWallet.setId(2);
            List<Wallet> multipleWallets = Arrays.asList(testWallet, otherWallet);
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(multipleWallets);
            
            // Mock other dependencies
            doNothing().when(budgetWalletRepository).deleteBudgetWalletByWallet(1);
            when(transactionRepository.listTransactionByAllWallet(1, testUser.getUserId()))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.saveAll(Collections.emptyList()))
                    .thenReturn(Collections.emptyList());

            walletService.deleteWallet(1);

            assertTrue(testWallet.getIsDeleted(), "Wallet should be marked as deleted");
            assertNotNull(testWallet.getDeletedAt(), "DeletedAt should be set");
            assertTrue(testWallet.getIsDefault(), "Wallet should remain default");

            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletRepository, times(2)).findAllActiveByUserId(testUser.getUserId()); // Called twice: once for validation, once for setting another wallet as default
            verify(budgetWalletRepository).deleteBudgetWalletByWallet(1);
            verify(transactionRepository).listTransactionByAllWallet(1, testUser.getUserId());
            verify(transactionRepository).saveAll(Collections.emptyList());
            verify(walletRepository, times(2)).save(any(Wallet.class)); // Called twice: once for setting another wallet as default, once for the deleted wallet
        }
    }



    @Test
    void deleteWallet_Success_DeletesNonDefaultWallet() {
        // Given
        testWallet.setIsDefault(false);

        try (MockedStatic<UserUtils> mocked = mockStatic(UserUtils.class)) {
            mocked.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            
            // Mock multiple active wallets so the "last wallet" validation doesn't trigger
            List<Wallet> multipleWallets = Arrays.asList(testWallet, new Wallet());
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(multipleWallets);
            
            // Mock other dependencies
            doNothing().when(budgetWalletRepository).deleteBudgetWalletByWallet(1);
            when(transactionRepository.listTransactionByAllWallet(1, testUser.getUserId()))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.saveAll(Collections.emptyList()))
                    .thenReturn(Collections.emptyList());

            // When
            walletService.deleteWallet(1);

            // Then
            assertTrue(testWallet.getIsDeleted(), "Wallet should be marked as deleted");
            assertNotNull(testWallet.getDeletedAt(), "DeletedAt should be set");
            assertFalse(testWallet.getIsDefault(), "Wallet should remain non-default");

            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(budgetWalletRepository).deleteBudgetWalletByWallet(1);
            verify(transactionRepository).listTransactionByAllWallet(1, testUser.getUserId());
            verify(transactionRepository).saveAll(Collections.emptyList());
            verify(walletRepository).save(testWallet);
        }
    }


    @Test
    void deleteWallet_Success_DeletesWalletWithZeroBalance() {
        // Given
        testWallet.setCurrentBalance(BigDecimal.ZERO);

        try (MockedStatic<UserUtils> mocked = mockStatic(UserUtils.class)) {
            mocked.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            
            // Mock multiple active wallets so the "last wallet" validation doesn't trigger
            List<Wallet> multipleWallets = Arrays.asList(testWallet, new Wallet());
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(multipleWallets);
            
            // Mock other dependencies
            doNothing().when(budgetWalletRepository).deleteBudgetWalletByWallet(1);
            when(transactionRepository.listTransactionByAllWallet(1, testUser.getUserId()))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.saveAll(Collections.emptyList()))
                    .thenReturn(Collections.emptyList());

            // When
            walletService.deleteWallet(1);

            // Then
            assertTrue(testWallet.getIsDeleted(), "Wallet should be marked as deleted");
            assertNotNull(testWallet.getDeletedAt(), "DeletedAt should be set");
            assertEquals(BigDecimal.ZERO, testWallet.getCurrentBalance(), "Balance should remain unchanged");

            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(budgetWalletRepository).deleteBudgetWalletByWallet(1);
            verify(transactionRepository).listTransactionByAllWallet(1, testUser.getUserId());
            verify(transactionRepository).saveAll(Collections.emptyList());
            verify(walletRepository).save(testWallet);
        }
    }


    @Test
    void deleteWallet_Success_DeletesWalletWithLargeBalance() {
        // Given
        BigDecimal largeBalance = new BigDecimal("999999999.99");
        testWallet.setCurrentBalance(largeBalance);
        testWallet.setIsDefault(false);
        testWallet.setExcludeFromTotal(false);

        try (MockedStatic<UserUtils> mocked = mockStatic(UserUtils.class)) {
            mocked.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            
            // Mock multiple active wallets so the "last wallet" validation doesn't trigger
            List<Wallet> multipleWallets = Arrays.asList(testWallet, new Wallet());
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(multipleWallets);
            
            // Mock other dependencies
            doNothing().when(budgetWalletRepository).deleteBudgetWalletByWallet(1);
            when(transactionRepository.listTransactionByAllWallet(1, testUser.getUserId()))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.saveAll(Collections.emptyList()))
                    .thenReturn(Collections.emptyList());

            // When
            walletService.deleteWallet(1);

            // Then
            assertTrue(testWallet.getIsDeleted(), "Wallet should be marked as deleted");
            assertNotNull(testWallet.getDeletedAt(), "DeletedAt timestamp should be set");
            assertEquals(largeBalance, testWallet.getCurrentBalance(), "Balance should remain unchanged");

            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(budgetWalletRepository).deleteBudgetWalletByWallet(1);
            verify(transactionRepository).listTransactionByAllWallet(1, testUser.getUserId());
            verify(transactionRepository).saveAll(Collections.emptyList());
            verify(walletRepository).save(testWallet);
        }
    }


    @Test
    void deleteWallet_Success_DeletesExcludedFromTotalWallet() {
        // Given
        testWallet.setExcludeFromTotal(true);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            
            // Mock multiple active wallets so the "last wallet" validation doesn't trigger
            List<Wallet> multipleWallets = Arrays.asList(testWallet, new Wallet());
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(multipleWallets);

            // Mock các call khác để tránh lỗi khi chạy service
            doNothing().when(budgetWalletRepository).deleteBudgetWalletByWallet(1);
            when(transactionRepository.listTransactionByAllWallet(1, testUser.getUserId()))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.saveAll(Collections.emptyList()))
                    .thenReturn(Collections.emptyList());
            when(walletRepository.save(testWallet)).thenReturn(testWallet);

            // When
            walletService.deleteWallet(1);

            // Then
            assertTrue(testWallet.getIsDeleted());
            assertNotNull(testWallet.getDeletedAt());
            assertTrue(testWallet.getExcludeFromTotal()); // Should remain true

            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(budgetWalletRepository).deleteBudgetWalletByWallet(1);
            verify(transactionRepository).listTransactionByAllWallet(1, testUser.getUserId());
            verify(transactionRepository).saveAll(Collections.emptyList());
            verify(walletRepository).save(testWallet);
        }
    }


    @Test
    void deleteWallet_Success_VerifyTimestampIsSet() {
        // Given
        Instant beforeDelete = Instant.now();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            
            // Mock multiple active wallets so the "last wallet" validation doesn't trigger
            List<Wallet> multipleWallets = Arrays.asList(testWallet, new Wallet());
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(multipleWallets);

            // Mock các call khác để tránh NullPointer
            doNothing().when(budgetWalletRepository).deleteBudgetWalletByWallet(1);
            when(transactionRepository.listTransactionByAllWallet(1, testUser.getUserId()))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.saveAll(Collections.emptyList()))
                    .thenReturn(Collections.emptyList());
            when(walletRepository.save(testWallet)).thenReturn(testWallet);

            // When
            walletService.deleteWallet(1);

            // Then
            assertTrue(testWallet.getIsDeleted());
            assertNotNull(testWallet.getDeletedAt());
            assertTrue(
                    testWallet.getDeletedAt().isAfter(beforeDelete) ||
                            testWallet.getDeletedAt().equals(beforeDelete)
            );

            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(budgetWalletRepository).deleteBudgetWalletByWallet(1);
            verify(transactionRepository).listTransactionByAllWallet(1, testUser.getUserId());
            verify(transactionRepository).saveAll(Collections.emptyList());
            verify(walletRepository).save(testWallet);
        }
    }


    @Test
    void deleteWallet_ThrowsException_WhenCurrentUserIsNull() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(null);

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.deleteWallet(1)
            );
            assertEquals("Unable to identify the current user", exception.getMessage());
            
            verify(walletRepository, never()).findByIdAndNotDeleted(any());
            verify(walletRepository, never()).save(any());
        }
    }

    @Test
    void deleteWallet_ThrowsException_WhenWalletIdIsNull() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(null)).thenReturn(Optional.empty());

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.deleteWallet(null)
            );
            assertEquals("Wallet not found with id: null", exception.getMessage());
            
            verify(walletRepository).findByIdAndNotDeleted(null);
            verify(walletRepository, never()).save(any());
        }
    }

    @Test
    void deleteWallet_ThrowsException_WhenWalletIdIsZero() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(0)).thenReturn(Optional.empty());

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.deleteWallet(0)
            );
            assertEquals("Wallet not found with id: 0", exception.getMessage());
            
            verify(walletRepository).findByIdAndNotDeleted(0);
            verify(walletRepository, never()).save(any());
        }
    }

    @Test
    void deleteWallet_ThrowsException_WhenWalletIdIsNegative() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(-1)).thenReturn(Optional.empty());

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.deleteWallet(-1)
            );
            assertEquals("Wallet not found with id: -1", exception.getMessage());
            
            verify(walletRepository).findByIdAndNotDeleted(-1);
            verify(walletRepository, never()).save(any());
        }
    }

    @Test
    void deleteWallet_ThrowsException_WhenWalletNotFound() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.empty());

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.deleteWallet(1)
            );
            assertEquals("Wallet not found with id: 1", exception.getMessage());
        }
    }

    @Test
    void deleteWallet_ThrowsException_WhenWalletNotFoundWithLargeId() {
        // Given
        int largeId = Integer.MAX_VALUE;
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(largeId)).thenReturn(Optional.empty());

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.deleteWallet(largeId)
            );
            assertEquals("Wallet not found with id: " + largeId, exception.getMessage());
        }
    }

    @Test
    void deleteWallet_ThrowsException_WhenUserDoesNotOwnWallet() {
        // Given
        User anotherUser = User.builder()
                .userId(2)
                .userEmail("another@example.com")
                .build();
        testWallet.setUser(anotherUser);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));

            // When & Then
            WalletException exception = assertThrows(
                    WalletException.class,
                    () -> walletService.deleteWallet(1)
            );
            assertEquals("User does not own this wallet", exception.getMessage());
            
            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletRepository, never()).save(any());
        }
    }

    @Test
    void deleteWallet_ThrowsException_WhenWalletOwnerIsNull() {
        // Given
        testWallet.setUser(null);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));

            // When & Then
            assertThrows(
                    NullPointerException.class,
                    () -> walletService.deleteWallet(1)
            );
            
            verify(walletRepository).findByIdAndNotDeleted(1);
        }
    }

    @Test
    void deleteWallet_ThrowsException_WhenRepositoryThrowsException() {
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            // Given
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));

            // Mock multiple active wallets so the "last wallet" validation doesn't trigger
            List<Wallet> multipleWallets = Arrays.asList(testWallet, new Wallet());
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(multipleWallets);

            // Giả lập list transaction rỗng để skip setTransactionType
            when(transactionRepository.listTransactionByAllWallet(1, testUser.getUserId()))
                    .thenReturn(Collections.emptyList());

            // Mock lỗi khi save wallet
            when(walletRepository.save(testWallet))
                    .thenThrow(new RuntimeException("Database error"));

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> walletService.deleteWallet(1)
            );
            assertEquals("Database error", exception.getMessage());

            // Verify các call cần thiết
            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(budgetWalletRepository).deleteBudgetWalletByWallet(1);
            verify(transactionRepository).listTransactionByAllWallet(1, testUser.getUserId());
            verify(transactionRepository).saveAll(Collections.emptyList());
            verify(walletRepository).save(testWallet);
        }
    }


    @Test
    void deleteWallet_ThrowsException_WhenRepositoryFindThrowsException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenThrow(new RuntimeException("Database connection error"));

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> walletService.deleteWallet(1)
            );
            assertEquals("Database connection error", exception.getMessage());
            
            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletRepository, never()).save(any());
        }
    }
}