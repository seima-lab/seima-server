package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import vn.fpt.seima.seimaserver.dto.request.user.UserCreationRequestDto;
import vn.fpt.seima.seimaserver.dto.request.user.UserUpdateRequestDto;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.NotMatchCurrentGmailException;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.repository.UserDeviceRepository;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.impl.UserServiceImpl;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @Mock
    private UserDeviceService userDeviceService;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserCreationRequestDto userCreationRequest;
    private UserUpdateRequestDto userUpdateRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setUserId(1);
        testUser.setUserEmail("test@example.com");
        testUser.setUserFullName("Test User");
        testUser.setUserGender(true);
        testUser.setUserDob(LocalDate.of(1990, 1, 1));
        testUser.setUserPhoneNumber("0987654321");
        testUser.setUserIsActive(true);
        testUser.setUserAvatarUrl("http://example.com/avatar.jpg");

        // Setup UserCreationRequestDto
        userCreationRequest = new UserCreationRequestDto();
        userCreationRequest.setEmail("test@example.com");
        userCreationRequest.setFullName("Updated User");
        userCreationRequest.setBirthDate(LocalDate.of(1991, 2, 2));
        userCreationRequest.setPhoneNumber("0123456789");
        userCreationRequest.setGender(false);
        userCreationRequest.setAvatarUrl("http://example.com/new-avatar.jpg");
        userCreationRequest.setDeviceId("device123");
        userCreationRequest.setFcmToken("fcm_token_123");

        // Setup UserUpdateRequestDto
        userUpdateRequest = new UserUpdateRequestDto();
        userUpdateRequest.setFullName("Updated Name");
        userUpdateRequest.setBirthDate(LocalDate.of(1992, 3, 3));
        userUpdateRequest.setPhoneNumber("0111222333");
        userUpdateRequest.setGender(false);
    }

    // Tests for findUserById
    @Test
    void findUserById_WhenUserExists_ShouldReturnUser() {
        // Given
        Integer userId = 1;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.findUserById(userId);

        // Then
        assertNotNull(result);
        assertEquals(testUser.getUserId(), result.getUserId());
        assertEquals(testUser.getUserEmail(), result.getUserEmail());
        verify(userRepository).findById(userId);
    }

    @Test
    void findUserById_WhenUserNotExists_ShouldThrowResourceNotFoundException() {
        // Given
        Integer userId = 999;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> userService.findUserById(userId)
        );
        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(userId);
    }

    // Tests for processAddNewUser
    @Test
    void processAddNewUser_WhenValidRequest_ShouldUpdateUserSuccessfully() {
        // Given
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(userDeviceRepository.existsByDeviceId(userCreationRequest.getDeviceId())).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userService.processAddNewUser(userCreationRequest);

            // Then
            verify(userRepository).save(testUser);
            verify(userDeviceService).updateDeviceUser(testUser.getUserId(), userCreationRequest.getDeviceId(), userCreationRequest.getFcmToken());
            verify(userDeviceService, never()).createDevice(anyInt(), anyString(), anyString());
            
            assertEquals(userCreationRequest.getFullName(), testUser.getUserFullName());
            assertEquals(userCreationRequest.isGender(), testUser.getUserGender());
            assertEquals(userCreationRequest.getBirthDate(), testUser.getUserDob());
            assertEquals(userCreationRequest.getPhoneNumber(), testUser.getUserPhoneNumber());
            assertTrue(testUser.getUserIsActive());
        }
    }

    @Test
    void processAddNewUser_WhenRequestIsNull_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.processAddNewUser(null)
        );
        assertEquals("User creation request cannot be null", exception.getMessage());
    }

    @Test
    void processAddNewUser_WhenCurrentUserIsNull_ShouldThrowIllegalStateException() {
        // Given
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(null);

            // When & Then
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> userService.processAddNewUser(userCreationRequest)
            );
            assertEquals("Authenticated user not found. Cannot process user creation/update.", exception.getMessage());
        }
    }

    @Test
    void processAddNewUser_WhenEmailMismatch_ShouldThrowNotMatchCurrentGmailException() {
        // Given
        userCreationRequest.setEmail("different@example.com");
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(testUser);

            // When & Then
            NotMatchCurrentGmailException exception = assertThrows(
                NotMatchCurrentGmailException.class,
                () -> userService.processAddNewUser(userCreationRequest)
            );
            assertEquals("Email in request does not match the authenticated user's email.", exception.getMessage());
        }
    }

    @Test
    void processAddNewUser_WhenDeviceNotExists_ShouldCreateDeviceOnly() {
        // Given
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(userDeviceRepository.existsByDeviceId(userCreationRequest.getDeviceId())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userService.processAddNewUser(userCreationRequest);

            // Then
            verify(userDeviceService, never()).updateDeviceUser(anyInt(), anyString(), anyString());
            verify(userDeviceService).createDevice(testUser.getUserId(), userCreationRequest.getDeviceId(), userCreationRequest.getFcmToken());
        }
    }

    // Tests for deactivateUserAccount
    @Test
    void deactivateUserAccount_WhenValidUserId_ShouldDeactivateUser() {
        // Given
        Integer userId = 1;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.deactivateUserAccount(userId);

        // Then
        assertFalse(testUser.getUserIsActive());
        verify(userRepository).findById(userId);
        verify(userRepository).save(testUser);
    }

    @Test
    void deactivateUserAccount_WhenUserIdIsNull_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.deactivateUserAccount(null)
        );
        assertEquals("User ID cannot be null", exception.getMessage());
    }

    @Test
    void deactivateUserAccount_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        Integer userId = 999;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> userService.deactivateUserAccount(userId)
        );
        assertEquals("User not found with id: " + userId, exception.getMessage());
    }

    @Test
    void deactivateUserAccount_WhenUserAlreadyInactive_ShouldNotUpdateUser() {
        // Given
        Integer userId = 1;
        testUser.setUserIsActive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        userService.deactivateUserAccount(userId);

        // Then
        verify(userRepository, never()).save(any(User.class));
    }

    // Tests for updateUserProfileWithImage
    @Test
    void updateUserProfileWithImage_WhenValidRequest_ShouldUpdateUser() {
        // Given
        Integer userId = 1;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUserProfileWithImage(userId, userUpdateRequest);

        // Then
        assertNotNull(result);
        assertEquals(userUpdateRequest.getFullName(), testUser.getUserFullName());
        assertEquals(userUpdateRequest.getBirthDate(), testUser.getUserDob());
        assertEquals(userUpdateRequest.getPhoneNumber(), testUser.getUserPhoneNumber());
        assertEquals(userUpdateRequest.getGender(), testUser.getUserGender());
        assertTrue(testUser.getUserIsActive());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserProfileWithImage_WhenUserIdIsNull_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updateUserProfileWithImage(null, userUpdateRequest)
        );
        assertEquals("User ID cannot be null", exception.getMessage());
    }

    @Test
    void updateUserProfileWithImage_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        Integer userId = 999;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> userService.updateUserProfileWithImage(userId, userUpdateRequest)
        );
        assertEquals("User not found with id: " + userId, exception.getMessage());
    }

    @Test
    void updateUserProfileWithImage_WhenRemoveAvatar_ShouldRemoveAvatarUrl() {
        // Given
        Integer userId = 1;
        userUpdateRequest.setRemoveCurrentAvatar(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUserProfileWithImage(userId, userUpdateRequest);

        // Then
        assertNull(testUser.getUserAvatarUrl());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserProfileWithImage_WhenUploadNewImage_ShouldUpdateAvatarUrl() {
        // Given
        Integer userId = 1;
        MockMultipartFile imageFile = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", "test image content".getBytes()
        );
        userUpdateRequest.setImage(imageFile);
        
        Map<String, Object> uploadResult = Map.of("secure_url", "http://example.com/new-uploaded-avatar.jpg");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(cloudinaryService.uploadImage(any(MultipartFile.class), eq("users/avatars"))).thenReturn(uploadResult);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUserProfileWithImage(userId, userUpdateRequest);

        // Then
        assertEquals("http://example.com/new-uploaded-avatar.jpg", testUser.getUserAvatarUrl());
        verify(cloudinaryService).uploadImage(imageFile, "users/avatars");
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserProfileWithImage_WhenImageTooLarge_ShouldThrowIllegalArgumentException() {
        // Given
        Integer userId = 1;
        // Create a file larger than 5MB
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile largeImageFile = new MockMultipartFile(
            "image", "large.jpg", "image/jpeg", largeContent
        );
        userUpdateRequest.setImage(largeImageFile);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updateUserProfileWithImage(userId, userUpdateRequest)
        );
        assertEquals("Image file size must be less than 5MB", exception.getMessage());
    }

    @Test
    void updateUserProfileWithImage_WhenInvalidImageFormat_ShouldThrowIllegalArgumentException() {
        // Given
        Integer userId = 1;
        MockMultipartFile invalidImageFile = new MockMultipartFile(
            "image", "test.txt", "text/plain", "test content".getBytes()
        );
        userUpdateRequest.setImage(invalidImageFile);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updateUserProfileWithImage(userId, userUpdateRequest)
        );
        assertEquals("Unsupported image format. Supported formats: jpg, jpeg, png, gif, webp", exception.getMessage());
    }

    @Test
    void updateUserProfileWithImage_WhenInvalidMimeType_ShouldThrowIllegalArgumentException() {
        // Given
        Integer userId = 1;
        // File có extension hợp lệ nhưng MIME type không phải image
        MockMultipartFile invalidMimeFile = new MockMultipartFile(
            "image", "test.jpg", "text/plain", "test content".getBytes()
        );
        userUpdateRequest.setImage(invalidMimeFile);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updateUserProfileWithImage(userId, userUpdateRequest)
        );
        assertEquals("File must be an image", exception.getMessage());
    }

    @Test
    void updateUserProfileWithImage_WhenConflictingImageRequest_ShouldThrowIllegalArgumentException() {
        // Given
        Integer userId = 1;
        MockMultipartFile imageFile = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", "test image content".getBytes()
        );
        userUpdateRequest.setImage(imageFile);
        userUpdateRequest.setRemoveCurrentAvatar(true);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updateUserProfileWithImage(userId, userUpdateRequest)
        );
        assertEquals("Cannot provide new image and remove current avatar at the same time", exception.getMessage());
    }

    @Test
    void updateUserProfileWithImage_WhenCloudinaryUploadFails_ShouldThrowRuntimeException() {
        // Given
        Integer userId = 1;
        MockMultipartFile imageFile = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", "test image content".getBytes()
        );
        userUpdateRequest.setImage(imageFile);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(cloudinaryService.uploadImage(any(MultipartFile.class), eq("users/avatars")))
            .thenThrow(new RuntimeException("Cloudinary upload failed"));

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userService.updateUserProfileWithImage(userId, userUpdateRequest)
        );
        assertTrue(exception.getMessage().contains("Failed to upload avatar"));
    }
}

