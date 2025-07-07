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
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.entity.Wallet;
import vn.fpt.seima.seimaserver.entity.WalletType;
import vn.fpt.seima.seimaserver.exception.WalletException;
import vn.fpt.seima.seimaserver.mapper.WalletMapper;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.repository.WalletRepository;
import vn.fpt.seima.seimaserver.repository.WalletTypeRepository;
import vn.fpt.seima.seimaserver.service.impl.WalletServiceImpl;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
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

    @InjectMocks
    private WalletServiceImpl walletService;

    // Test data
    private User testUser;
    private WalletType testWalletType;
    private Wallet testWallet;
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

        // Setup test wallet
        testWallet = new Wallet();
        testWallet.setId(1);
        testWallet.setWalletName("Test Wallet");
        testWallet.setCurrentBalance(new BigDecimal("1000.00"));
        testWallet.setDescription("Test wallet description");
        testWallet.setIsDefault(false);
        testWallet.setExcludeFromTotal(false);
        testWallet.setBankName("Test Bank");
        testWallet.setIconUrl("https://example.com/icon.png");
        testWallet.setIsDeleted(false);
        testWallet.setWalletCreatedAt(Instant.now());
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
                .bankName("Test Bank")
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
                .bankName("Test Bank")
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
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            
            // Mock for wallet name uniqueness validation (name doesn't exist for other wallets)
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), updateRequest.getWalletName(), 1)).thenReturn(false);
            
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            when(walletRepository.save(testWallet)).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.updateWallet(1, updateRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletRepository).existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), updateRequest.getWalletName(), 1);
            verify(walletTypeRepository).findById(1);
            verify(walletMapper).updateEntity(testWallet, updateRequest);
            verify(walletRepository).save(testWallet);
            verify(walletMapper).toResponse(testWallet);
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
                .build();

        List<Wallet> existingWallets = Arrays.asList(testWallet);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));
            
            // Mock for wallet name uniqueness validation (name doesn't exist for other wallets)
            when(walletRepository.existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), updateRequest.getWalletName(), 1)).thenReturn(false);
            
            when(walletTypeRepository.findById(1)).thenReturn(Optional.of(testWalletType));
            when(walletRepository.findAllActiveByUserId(testUser.getUserId())).thenReturn(existingWallets);
            when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
            when(walletMapper.toResponse(testWallet)).thenReturn(walletResponse);

            // When
            WalletResponse result = walletService.updateWallet(1, updateRequest);

            // Then
            assertNotNull(result);
            verify(walletRepository).existsByUserIdAndWalletNameAndNotDeletedAndIdNot(testUser.getUserId(), updateRequest.getWalletName(), 1);
            verify(walletRepository).findAllActiveByUserId(testUser.getUserId());
            verify(walletRepository, times(2)).save(any(Wallet.class)); // Once for existing wallets, once for updated wallet
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

    // ===== DELETE WALLET TESTS =====

    @Test
    void deleteWallet_Success_SoftDeletesWallet() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(walletRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testWallet));

            // When
            walletService.deleteWallet(1);

            // Then
            assertTrue(testWallet.getIsDeleted());
            assertNotNull(testWallet.getDeletedAt());
            verify(walletRepository).findByIdAndNotDeleted(1);
            verify(walletRepository).save(testWallet);
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
        }
    }
}