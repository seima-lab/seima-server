package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.entity.UserDevice;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.repository.UserDeviceRepository;
import vn.fpt.seima.seimaserver.service.impl.UserDeviceServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDeviceServiceTest {

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserDeviceServiceImpl userDeviceService;

    private User testUser;
    private UserDevice testUserDevice;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1)
                .userEmail("test@example.com")
                .userFullName("Test User")
                .userIsActive(true)
                .build();

        testUserDevice = UserDevice.builder()
                .id(1)
                .deviceId("device123")
                .fcmToken("fcm_token_123")
                .user(testUser)
                .lastChange(LocalDateTime.now())
                .build();
    }

    // ================= CREATE DEVICE TESTS =================

    @Test
    void createDevice_WhenValidInputs_ShouldCreateDeviceSuccessfully() {
        // Given
        Integer userId = 1;
        String deviceId = "device123";
        String fcmToken = "fcm_token_123";

        when(userService.findUserById(userId)).thenReturn(testUser);
        when(userDeviceRepository.save(any(UserDevice.class))).thenReturn(testUserDevice);

        // When
        UserDevice result = userDeviceService.createDevice(userId, deviceId, fcmToken);

        // Then
        assertNotNull(result);
        assertEquals(testUserDevice.getId(), result.getId());
        assertEquals(testUserDevice.getDeviceId(), result.getDeviceId());
        assertEquals(testUserDevice.getFcmToken(), result.getFcmToken());
        assertEquals(testUser, result.getUser());

        verify(userService).findUserById(userId);
        verify(userDeviceRepository).save(any(UserDevice.class));
    }

    @Test
    void createDevice_WhenUserIdIsNull_ShouldThrowIllegalArgumentException() {
        // Given
        Integer userId = null;
        String deviceId = "device123";
        String fcmToken = "fcm_token_123";

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDeviceService.createDevice(userId, deviceId, fcmToken)
        );

        assertEquals("User ID cannot be null", exception.getMessage());
        verify(userService, never()).findUserById(any());
        verify(userDeviceRepository, never()).save(any());
    }

    @Test
    void createDevice_WhenDeviceIdIsNull_ShouldThrowIllegalArgumentException() {
        // Given
        Integer userId = 1;
        String deviceId = null;
        String fcmToken = "fcm_token_123";

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDeviceService.createDevice(userId, deviceId, fcmToken)
        );

        assertEquals("Device ID cannot be null or empty", exception.getMessage());
        verify(userService, never()).findUserById(any());
        verify(userDeviceRepository, never()).save(any());
    }

    @Test
    void createDevice_WhenDeviceIdIsEmpty_ShouldThrowIllegalArgumentException() {
        // Given
        Integer userId = 1;
        String deviceId = "   ";
        String fcmToken = "fcm_token_123";

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDeviceService.createDevice(userId, deviceId, fcmToken)
        );

        assertEquals("Device ID cannot be null or empty", exception.getMessage());
        verify(userService, never()).findUserById(any());
        verify(userDeviceRepository, never()).save(any());
    }

    @Test
    void createDevice_WhenFcmTokenIsNull_ShouldThrowIllegalArgumentException() {
        // Given
        Integer userId = 1;
        String deviceId = "device123";
        String fcmToken = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDeviceService.createDevice(userId, deviceId, fcmToken)
        );

        assertEquals("FCM token cannot be null or empty", exception.getMessage());
        verify(userService, never()).findUserById(any());
        verify(userDeviceRepository, never()).save(any());
    }

    @Test
    void createDevice_WhenFcmTokenIsEmpty_ShouldThrowIllegalArgumentException() {
        // Given
        Integer userId = 1;
        String deviceId = "device123";
        String fcmToken = "   ";

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDeviceService.createDevice(userId, deviceId, fcmToken)
        );

        assertEquals("FCM token cannot be null or empty", exception.getMessage());
        verify(userService, never()).findUserById(any());
        verify(userDeviceRepository, never()).save(any());
    }

    @Test
    void createDevice_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        Integer userId = 999;
        String deviceId = "device123";
        String fcmToken = "fcm_token_123";

        when(userService.findUserById(userId)).thenThrow(new ResourceNotFoundException("User not found"));

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userDeviceService.createDevice(userId, deviceId, fcmToken)
        );

        assertEquals("User not found", exception.getMessage());
        verify(userService).findUserById(userId);
        verify(userDeviceRepository, never()).save(any());
    }

    @Test
    void createDevice_WhenInputsHaveWhitespace_ShouldTrimAndCreateDevice() {
        // Given
        Integer userId = 1;
        String deviceId = "  device123  ";
        String fcmToken = "  fcm_token_123  ";

        when(userService.findUserById(userId)).thenReturn(testUser);
        when(userDeviceRepository.save(any(UserDevice.class))).thenReturn(testUserDevice);

        // When
        UserDevice result = userDeviceService.createDevice(userId, deviceId, fcmToken);

        // Then
        assertNotNull(result);
        verify(userService).findUserById(userId);
        verify(userDeviceRepository).save(argThat(device -> 
            device.getDeviceId().equals("device123") && 
            device.getFcmToken().equals("fcm_token_123")
        ));
    }

    // ================= UPDATE DEVICE USER TESTS =================

    @Test
    void updateDeviceUser_WhenValidInputs_ShouldUpdateDeviceSuccessfully() {
        // Given
        Integer userId = 1;
        String deviceId = "device123";
        String newFcmToken = "new_fcm_token_456";

        UserDevice updatedDevice = UserDevice.builder()
                .id(1)
                .deviceId(deviceId)
                .fcmToken(newFcmToken)
                .user(testUser)
                .lastChange(LocalDateTime.now())
                .build();

        when(userDeviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(testUserDevice));
        when(userService.findUserById(userId)).thenReturn(testUser);
        when(userDeviceRepository.save(any(UserDevice.class))).thenReturn(updatedDevice);

        // When
        UserDevice result = userDeviceService.updateDeviceUser(userId, deviceId, newFcmToken);

        // Then
        assertNotNull(result);
        assertEquals(newFcmToken, result.getFcmToken());
        assertEquals(deviceId, result.getDeviceId());

        verify(userDeviceRepository).findByDeviceId(deviceId);
        verify(userService).findUserById(userId);
        verify(userDeviceRepository).save(any(UserDevice.class));
    }

    @Test
    void updateDeviceUser_WhenUserIdIsNull_ShouldThrowIllegalArgumentException() {
        // Given
        Integer userId = null;
        String deviceId = "device123";
        String fcmToken = "new_fcm_token_456";

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDeviceService.updateDeviceUser(userId, deviceId, fcmToken)
        );

        assertEquals("User ID cannot be null", exception.getMessage());
        verify(userDeviceRepository, never()).findByDeviceId(any());
        verify(userService, never()).findUserById(any());
        verify(userDeviceRepository, never()).save(any());
    }

    @Test
    void updateDeviceUser_WhenDeviceIdIsNull_ShouldThrowIllegalArgumentException() {
        // Given
        Integer userId = 1;
        String deviceId = null;
        String fcmToken = "new_fcm_token_456";

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDeviceService.updateDeviceUser(userId, deviceId, fcmToken)
        );

        assertEquals("Existing device cannot be null", exception.getMessage());
        verify(userDeviceRepository, never()).findByDeviceId(any());
        verify(userService, never()).findUserById(any());
        verify(userDeviceRepository, never()).save(any());
    }

    @Test
    void updateDeviceUser_WhenFcmTokenIsNull_ShouldThrowIllegalArgumentException() {
        // Given
        Integer userId = 1;
        String deviceId = "device123";
        String fcmToken = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDeviceService.updateDeviceUser(userId, deviceId, fcmToken)
        );

        assertEquals("FCM token cannot be null or empty", exception.getMessage());
        verify(userDeviceRepository, never()).findByDeviceId(any());
        verify(userService, never()).findUserById(any());
        verify(userDeviceRepository, never()).save(any());
    }

    @Test
    void updateDeviceUser_WhenFcmTokenIsEmpty_ShouldThrowIllegalArgumentException() {
        // Given
        Integer userId = 1;
        String deviceId = "device123";
        String fcmToken = "   ";

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDeviceService.updateDeviceUser(userId, deviceId, fcmToken)
        );

        assertEquals("FCM token cannot be null or empty", exception.getMessage());
        verify(userDeviceRepository, never()).findByDeviceId(any());
        verify(userService, never()).findUserById(any());
        verify(userDeviceRepository, never()).save(any());
    }

    @Test
    void updateDeviceUser_WhenDeviceNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        Integer userId = 1;
        String deviceId = "nonexistent_device";
        String fcmToken = "new_fcm_token_456";

        when(userDeviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userDeviceService.updateDeviceUser(userId, deviceId, fcmToken)
        );

        assertEquals("Device not found with id: " + deviceId, exception.getMessage());
        verify(userDeviceRepository).findByDeviceId(deviceId);
        verify(userService, never()).findUserById(any());
        verify(userDeviceRepository, never()).save(any());
    }

    @Test
    void updateDeviceUser_WhenFcmTokenHasWhitespace_ShouldTrimAndUpdate() {
        // Given
        Integer userId = 1;
        String deviceId = "device123";
        String fcmToken = "  new_fcm_token_456  ";

        when(userDeviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(testUserDevice));
        when(userService.findUserById(userId)).thenReturn(testUser);
        when(userDeviceRepository.save(any(UserDevice.class))).thenReturn(testUserDevice);

        // When
        userDeviceService.updateDeviceUser(userId, deviceId, fcmToken);

        // Then
        verify(userDeviceRepository).findByDeviceId(deviceId);
        verify(userService).findUserById(userId);
        verify(userDeviceRepository).save(argThat(device ->
            device.getFcmToken().equals("new_fcm_token_456")
        ));
    }

    // ================= UPDATE USER ID TO NULL TESTS =================

    @Test
    void updateUserIdToNull_WhenValidDeviceId_ShouldUpdateSuccessfully() {
        // Given
        String deviceId = "device123";
        
        UserDevice updatedDevice = UserDevice.builder()
                .id(1)
                .deviceId(deviceId)
                .fcmToken(testUserDevice.getFcmToken())
                .user(null)
                .lastChange(LocalDateTime.now())
                .build();

        when(userDeviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(testUserDevice));
        when(userDeviceRepository.save(any(UserDevice.class))).thenReturn(updatedDevice);

        // When
        UserDevice result = userDeviceService.updateUserIdToNull(deviceId);

        // Then
        assertNotNull(result);
        assertNull(result.getUser());
        assertEquals(deviceId, result.getDeviceId());

        verify(userDeviceRepository).findByDeviceId(deviceId);
        verify(userDeviceRepository).save(any(UserDevice.class));
    }

    @Test
    void updateUserIdToNull_WhenDeviceIdIsNull_ShouldThrowIllegalArgumentException() {
        // Given
        String deviceId = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDeviceService.updateUserIdToNull(deviceId)
        );

        assertEquals("Device ID cannot be null or empty", exception.getMessage());
        verify(userDeviceRepository, never()).findByDeviceId(any());
        verify(userDeviceRepository, never()).save(any());
    }

    @Test
    void updateUserIdToNull_WhenDeviceIdIsEmpty_ShouldThrowIllegalArgumentException() {
        // Given
        String deviceId = "   ";

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDeviceService.updateUserIdToNull(deviceId)
        );

        assertEquals("Device ID cannot be null or empty", exception.getMessage());
        verify(userDeviceRepository, never()).findByDeviceId(any());
        verify(userDeviceRepository, never()).save(any());
    }

    @Test
    void updateUserIdToNull_WhenDeviceNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        String deviceId = "nonexistent_device";

        when(userDeviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userDeviceService.updateUserIdToNull(deviceId)
        );

        assertEquals("Device not found with id: " + deviceId, exception.getMessage());
        verify(userDeviceRepository).findByDeviceId(deviceId);
        verify(userDeviceRepository, never()).save(any());
    }

    @Test
    void updateUserIdToNull_ShouldUpdateLastChangeTime() {
        // Given
        String deviceId = "device123";
        LocalDateTime beforeUpdate = LocalDateTime.now();

        when(userDeviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(testUserDevice));
        when(userDeviceRepository.save(any(UserDevice.class))).thenReturn(testUserDevice);

        // When
        userDeviceService.updateUserIdToNull(deviceId);

        // Then
        verify(userDeviceRepository).save(argThat(device -> {
            assertNotNull(device.getLastChange());
            assertTrue(device.getLastChange().isAfter(beforeUpdate) || device.getLastChange().isEqual(beforeUpdate));
            return true;
        }));
    }

    // ================= INTEGRATION SCENARIO TESTS =================

    @Test
    void deviceLifecycle_CreateUpdateAndLogout_ShouldWorkCorrectly() {
        // Given - Create device
        Integer userId = 1;
        String deviceId = "device123";
        String initialFcmToken = "initial_token";
        String updatedFcmToken = "updated_token";

        when(userService.findUserById(userId)).thenReturn(testUser);
        when(userDeviceRepository.save(any(UserDevice.class))).thenReturn(testUserDevice);
        when(userDeviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(testUserDevice));

        // When - Create device
        UserDevice createdDevice = userDeviceService.createDevice(userId, deviceId, initialFcmToken);

        // Then - Verify creation
        assertNotNull(createdDevice);
        assertEquals(testUser, createdDevice.getUser());

        // When - Update FCM token
        UserDevice updatedDevice = userDeviceService.updateDeviceUser(userId, deviceId, updatedFcmToken);

        // Then - Verify update
        assertNotNull(updatedDevice);

        // When - Logout (set user to null)
        UserDevice loggedOutDevice = userDeviceService.updateUserIdToNull(deviceId);

        // Then - Verify logout
        assertNotNull(loggedOutDevice);

        // Verify all interactions
        verify(userService, times(2)).findUserById(userId);
        verify(userDeviceRepository, times(3)).save(any(UserDevice.class));
        verify(userDeviceRepository, times(2)).findByDeviceId(deviceId);
    }

    @Test
    void updateDeviceUser_WhenUserDeviceHasNullUser_ShouldStillUpdate() {
        // Given
        Integer userId = 1;
        String deviceId = "device123";
        String newFcmToken = "new_token";
        
        UserDevice deviceWithNullUser = UserDevice.builder()
                .id(1)
                .deviceId(deviceId)
                .fcmToken("old_token")
                .user(null)
                .lastChange(LocalDateTime.now().minusHours(1))
                .build();

        when(userDeviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(deviceWithNullUser));
        when(userService.findUserById(userId)).thenReturn(testUser);
        when(userDeviceRepository.save(any(UserDevice.class))).thenReturn(deviceWithNullUser);

        // When
        UserDevice result = userDeviceService.updateDeviceUser(userId, deviceId, newFcmToken);

        // Then
        assertNotNull(result);
        verify(userDeviceRepository).findByDeviceId(deviceId);
        verify(userService).findUserById(userId);
        verify(userDeviceRepository).save(argThat(device ->
            device.getFcmToken().equals(newFcmToken.trim())
        ));
    }
}
