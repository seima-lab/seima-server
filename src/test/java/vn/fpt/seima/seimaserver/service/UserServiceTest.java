package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import vn.fpt.seima.seimaserver.dto.request.user.UserUpdateRequestDto;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.impl.UserServiceImpl;

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

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserUpdateRequestDto validUpdateRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setUserId(1);
        testUser.setUserFullName("John Doe");
        testUser.setUserEmail("john@example.com");
        testUser.setUserIsActive(true);
        testUser.setUserAvatarUrl("https://old-avatar.com/image.jpg");

        // Setup valid update request
        validUpdateRequest = new UserUpdateRequestDto();
    }

    // ===== UPDATE USER PROFILE WITH IMAGE TESTS =====

    @Test
    void updateUserProfileWithImage_Success_WithImageUpload() {
        // Given
        validUpdateRequest.setFullName("Jane Doe Updated");
        validUpdateRequest.setBirthDate(LocalDate.of(1990, 1, 1));
        validUpdateRequest.setPhoneNumber("0912345678");
        validUpdateRequest.setGender(true);
        
        MockMultipartFile mockFile = new MockMultipartFile(
            "image", 
            "avatar.jpg", 
            "image/jpeg", 
            "test image content".getBytes()
        );
        validUpdateRequest.setImage(mockFile);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(cloudinaryService.uploadImage(any(MultipartFile.class), anyString()))
            .thenReturn(Map.of("secure_url", "https://new-avatar.com/image.jpg"));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUserProfileWithImage(1, validUpdateRequest);

        // Then
        assertNotNull(result);
        assertEquals("Jane Doe Updated", result.getUserFullName());
        assertEquals(LocalDate.of(1990, 1, 1), result.getUserDob());
        assertEquals("0912345678", result.getUserPhoneNumber());
        assertEquals(true, result.getUserGender());
        assertEquals("https://new-avatar.com/image.jpg", result.getUserAvatarUrl());
        assertTrue(result.getUserIsActive());

        verify(userRepository).findById(1);
        verify(cloudinaryService).uploadImage(mockFile, "users/avatars");
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserProfileWithImage_Success_WithRemoveAvatar() {
        // Given
        validUpdateRequest.setFullName("Jane Doe");
        validUpdateRequest.setRemoveCurrentAvatar(true);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUserProfileWithImage(1, validUpdateRequest);

        // Then
        assertNotNull(result);
        assertEquals("Jane Doe", result.getUserFullName());
        assertNull(result.getUserAvatarUrl()); // Avatar should be removed
        assertTrue(result.getUserIsActive());

        verify(userRepository).findById(1);
        verify(userRepository).save(testUser);
        verifyNoInteractions(cloudinaryService); // No image upload should occur
    }

    @Test
    void updateUserProfileWithImage_Success_WithBasicFieldsOnly() {
        // Given
        validUpdateRequest.setFullName("Jane Doe");
        validUpdateRequest.setBirthDate(LocalDate.of(1995, 5, 15));
        validUpdateRequest.setPhoneNumber("0987654321");
        validUpdateRequest.setGender(false);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUserProfileWithImage(1, validUpdateRequest);

        // Then
        assertNotNull(result);
        assertEquals("Jane Doe", result.getUserFullName());
        assertEquals(LocalDate.of(1995, 5, 15), result.getUserDob());
        assertEquals("0987654321", result.getUserPhoneNumber());
        assertEquals(false, result.getUserGender());
        assertEquals("https://old-avatar.com/image.jpg", result.getUserAvatarUrl()); // Avatar unchanged
        assertTrue(result.getUserIsActive());

        verify(userRepository).findById(1);
        verify(userRepository).save(testUser);
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void updateUserProfileWithImage_ThrowsException_WhenUserIdIsNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updateUserProfileWithImage(null, validUpdateRequest)
        );

        assertEquals("User ID cannot be null", exception.getMessage());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void updateUserProfileWithImage_ThrowsException_WhenUserNotFound() {
        // Given
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> userService.updateUserProfileWithImage(999, validUpdateRequest)
        );

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(userRepository).findById(999);
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void updateUserProfileWithImage_ThrowsException_WhenImageAndRemoveAvatarBothProvided() {
        // Given
        MockMultipartFile mockFile = new MockMultipartFile(
            "image", 
            "avatar.jpg", 
            "image/jpeg", 
            "test image content".getBytes()
        );
        validUpdateRequest.setImage(mockFile);
        validUpdateRequest.setRemoveCurrentAvatar(true); // Conflict!

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updateUserProfileWithImage(1, validUpdateRequest)
        );

        assertEquals("Cannot provide new image and remove current avatar at the same time", exception.getMessage());
        verify(userRepository).findById(1);
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void updateUserProfileWithImage_ThrowsException_WhenImageFormatInvalid() {
        // Given
        MockMultipartFile invalidFile = new MockMultipartFile(
            "image", 
            "document.pdf", 
            "application/pdf", 
            "not an image".getBytes()
        );
        validUpdateRequest.setImage(invalidFile);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updateUserProfileWithImage(1, validUpdateRequest)
        );

        assertEquals("Unsupported image format. Supported formats: jpg, jpeg, png, gif, webp", exception.getMessage());
        verify(userRepository).findById(1);
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void updateUserProfileWithImage_ThrowsException_WhenMimeTypeInvalid() {
        // Given - valid extension but invalid MIME type
        MockMultipartFile invalidFile = new MockMultipartFile(
            "image", 
            "fake-image.jpg", 
            "text/plain", 
            "not an image".getBytes()
        );
        validUpdateRequest.setImage(invalidFile);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updateUserProfileWithImage(1, validUpdateRequest)
        );

        assertEquals("File must be an image", exception.getMessage());
        verify(userRepository).findById(1);
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void updateUserProfileWithImage_ThrowsException_WhenImageSizeExceedsLimit() {
        // Given
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB - exceeds 5MB limit
        MockMultipartFile largeFile = new MockMultipartFile(
            "image", 
            "large.jpg", 
            "image/jpeg", 
            largeContent
        );
        validUpdateRequest.setImage(largeFile);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updateUserProfileWithImage(1, validUpdateRequest)
        );

        assertEquals("Image file size must be less than 5MB", exception.getMessage());
        verify(userRepository).findById(1);
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void updateUserProfileWithImage_ThrowsException_WhenCloudinaryUploadFails() {
        // Given
        MockMultipartFile mockFile = new MockMultipartFile(
            "image", 
            "avatar.jpg", 
            "image/jpeg", 
            "test image content".getBytes()
        );
        validUpdateRequest.setImage(mockFile);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(cloudinaryService.uploadImage(any(MultipartFile.class), anyString()))
            .thenThrow(new RuntimeException("Cloudinary error"));

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userService.updateUserProfileWithImage(1, validUpdateRequest)
        );

        assertEquals("Failed to upload avatar: Cloudinary error", exception.getMessage());
        verify(userRepository).findById(1);
        verify(cloudinaryService).uploadImage(mockFile, "users/avatars");
    }
} 