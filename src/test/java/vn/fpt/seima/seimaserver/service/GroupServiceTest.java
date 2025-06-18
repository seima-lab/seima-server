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
import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupResponse;
import vn.fpt.seima.seimaserver.entity.Group;
import vn.fpt.seima.seimaserver.entity.GroupMember;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.GroupException;
import vn.fpt.seima.seimaserver.mapper.GroupMapper;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.repository.GroupRepository;
import vn.fpt.seima.seimaserver.service.impl.GroupServiceImpl;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupMapper groupMapper;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private GroupServiceImpl groupService;

    private CreateGroupRequest validRequest;
    private User mockUser;
    private Group mockGroup;
    private GroupResponse mockResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        validRequest = new CreateGroupRequest();
        validRequest.setGroupName("Test Group");
        validRequest.setGroupAvatarUrl("http://example.com/avatar.jpg");

        mockUser = new User();
        mockUser.setUserId(1);
        mockUser.setUserEmail("test@example.com");

        mockGroup = new Group();
        mockGroup.setGroupId(1);
        mockGroup.setGroupName("Test Group");
        mockGroup.setGroupIsActive(true);

        mockResponse = new GroupResponse();
        mockResponse.setGroupId(1);
        mockResponse.setGroupName("Test Group");
    }

    @Test
    void createGroupWithImage_Success_WithoutImage() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupMapper.toEntity(validRequest)).thenReturn(mockGroup);
            when(groupRepository.save(any(Group.class))).thenReturn(mockGroup);
            when(groupMemberRepository.save(any(GroupMember.class))).thenReturn(new GroupMember());
            when(groupMapper.toResponse(mockGroup)).thenReturn(mockResponse);

            // When
            GroupResponse result = groupService.createGroupWithImage(validRequest);

            // Then
            assertNotNull(result);
            assertEquals("Test Group", result.getGroupName());
            verify(groupRepository).save(any(Group.class));
            verify(groupMemberRepository).save(any(GroupMember.class));
            verify(cloudinaryService, never()).uploadImage(any(), any());
        }
    }

    @Test
    void createGroupWithImage_Success_WithImage() {
        // Given
        MockMultipartFile imageFile = new MockMultipartFile(
                "image", 
                "test.jpg", 
                "image/jpeg", 
                "test image content".getBytes()
        );
        validRequest.setImage(imageFile);
        
        String uploadedImageUrl = "https://cloudinary.com/uploaded-image.jpg";
        Map<String, Object> uploadResult = Map.of("secure_url", uploadedImageUrl);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupMapper.toEntity(validRequest)).thenReturn(mockGroup);
            when(cloudinaryService.uploadImage(imageFile, "groups")).thenReturn(uploadResult);
            when(groupRepository.save(any(Group.class))).thenReturn(mockGroup);
            when(groupMemberRepository.save(any(GroupMember.class))).thenReturn(new GroupMember());
            when(groupMapper.toResponse(mockGroup)).thenReturn(mockResponse);

            // When
            GroupResponse result = groupService.createGroupWithImage(validRequest);

            // Then
            assertNotNull(result);
            verify(cloudinaryService).uploadImage(imageFile, "groups");
            verify(groupRepository).save(any(Group.class));
            verify(groupMemberRepository).save(any(GroupMember.class));
        }
    }

    @Test
    void createGroupWithImage_ThrowsException_WhenRequestIsNull() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
            () -> groupService.createGroupWithImage(null));
        
        assertEquals("Group request cannot be null", exception.getMessage());
    }

    @Test
    void createGroupWithImage_ThrowsException_WhenGroupNameIsEmpty() {
        // Given
        validRequest.setGroupName("");

        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
            () -> groupService.createGroupWithImage(validRequest));
        
        assertEquals("Group name is required and cannot be empty", exception.getMessage());
    }

    @Test
    void createGroupWithImage_ThrowsException_WhenGroupNameTooLong() {
        // Given
        String longName = "a".repeat(101); // 101 characters
        validRequest.setGroupName(longName);

        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
            () -> groupService.createGroupWithImage(validRequest));
        
        assertEquals("Group name cannot exceed 100 characters", exception.getMessage());
    }

    @Test
    void createGroupWithImage_ThrowsException_WhenUserNotFound() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(null);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                () -> groupService.createGroupWithImage(validRequest));
            
            assertEquals("Unable to identify the current user", exception.getMessage());
        }
    }

    @Test
    void createGroupWithImage_ThrowsException_WhenImageTooLarge() {
        // Given
        byte[] largeImageData = new byte[6 * 1024 * 1024]; // 6MB (exceeds 5MB limit)
        MockMultipartFile largeImageFile = new MockMultipartFile(
                "image", 
                "large.jpg", 
                "image/jpeg", 
                largeImageData
        );
        validRequest.setImage(largeImageFile);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                () -> groupService.createGroupWithImage(validRequest));
            
            assertEquals("Image file size must be less than 5MB", exception.getMessage());
        }
    }

    @Test
    void createGroupWithImage_ThrowsException_WhenImageFormatUnsupported() {
        // Given
        MockMultipartFile unsupportedFile = new MockMultipartFile(
                "image", 
                "test.pdf", 
                "application/pdf", 
                "pdf content".getBytes()
        );
        validRequest.setImage(unsupportedFile);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                () -> groupService.createGroupWithImage(validRequest));
            
            assertTrue(exception.getMessage().contains("Unsupported image format"));
        }
    }

    @Test
    void createGroupWithImage_ThrowsException_WhenImageMimeTypeInvalid() {
        // Given
        MockMultipartFile invalidMimeFile = new MockMultipartFile(
                "image", 
                "test.jpg", 
                "text/plain", // Invalid MIME type
                "fake image".getBytes()
        );
        validRequest.setImage(invalidMimeFile);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                () -> groupService.createGroupWithImage(validRequest));
            
            assertEquals("File must be an image", exception.getMessage());
        }
    }

    @Test
    void createGroupWithImage_ThrowsException_WhenCloudinaryUploadFails() {
        // Given
        MockMultipartFile imageFile = new MockMultipartFile(
                "image", 
                "test.jpg", 
                "image/jpeg", 
                "test image content".getBytes()
        );
        validRequest.setImage(imageFile);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(cloudinaryService.uploadImage(imageFile, "groups"))
                    .thenThrow(new RuntimeException("Upload failed"));

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                () -> groupService.createGroupWithImage(validRequest));
            
            assertTrue(exception.getMessage().contains("Failed to upload group avatar"));
        }
    }

    @Test
    void createGroupWithImage_Success_WithValidImageFormats() {
        // Test với các format hỗ trợ
        String[] supportedFormats = {"jpg", "jpeg", "png", "gif", "webp"};
        String[] mimeTypes = {"image/jpeg", "image/jpeg", "image/png", "image/gif", "image/webp"};
        
        for (int i = 0; i < supportedFormats.length; i++) {
            // Given
            MockMultipartFile imageFile = new MockMultipartFile(
                    "image", 
                    "test." + supportedFormats[i], 
                    mimeTypes[i], 
                    "test image content".getBytes()
            );
            validRequest.setImage(imageFile);
            
            Map<String, Object> uploadResult = Map.of("secure_url", "https://example.com/image.jpg");

            try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
                userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
                when(groupMapper.toEntity(validRequest)).thenReturn(mockGroup);
                when(cloudinaryService.uploadImage(imageFile, "groups")).thenReturn(uploadResult);
                when(groupRepository.save(any(Group.class))).thenReturn(mockGroup);
                when(groupMemberRepository.save(any(GroupMember.class))).thenReturn(new GroupMember());
                when(groupMapper.toResponse(mockGroup)).thenReturn(mockResponse);

                // When & Then - Should not throw exception
                assertDoesNotThrow(() -> groupService.createGroupWithImage(validRequest));
            }
        }
    }
} 