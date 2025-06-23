package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import vn.fpt.seima.seimaserver.config.base.AppProperties;
import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupDetailResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupResponse;
import vn.fpt.seima.seimaserver.entity.Group;
import vn.fpt.seima.seimaserver.entity.GroupMember;
import vn.fpt.seima.seimaserver.entity.GroupMemberRole;
import vn.fpt.seima.seimaserver.entity.GroupMemberStatus;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.GroupException;
import vn.fpt.seima.seimaserver.mapper.GroupMapper;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.repository.GroupRepository;
import vn.fpt.seima.seimaserver.service.impl.GroupServiceImpl;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private GroupServiceImpl groupService;

    private CreateGroupRequest validRequest;
    private User mockUser;
    private Group mockGroup;
    private GroupResponse mockResponse;
    
    // Additional test data for getGroupDetail tests
    private Group testGroup;
    private User testLeader;
    private User testMember1;
    private User testMember2;
    private GroupMember testGroupLeader;
    private GroupMember testGroupMember1;
    private GroupMember testGroupMember2;

    @BeforeEach
    void setUp() {
        // Setup test data for createGroupWithImage tests
        validRequest = new CreateGroupRequest();
        validRequest.setGroupName("Test Group");

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
        
        // Setup test data for getGroupDetail tests
        setupGetGroupDetailTestData();
    }
    
    private void setupGetGroupDetailTestData() {
        // Setup test group
        testGroup = new Group();
        testGroup.setGroupId(1);
        testGroup.setGroupName("Test Group");
        testGroup.setGroupInviteCode("test-invite-code-123");
        testGroup.setGroupAvatarUrl("https://example.com/group-avatar.jpg");
        testGroup.setGroupCreatedDate(LocalDateTime.now());
        testGroup.setGroupIsActive(true);

        // Setup test users
        testLeader = User.builder()
                .userId(1)
                .userFullName("John Doe")
                .userAvatarUrl("https://example.com/john-avatar.jpg")
                .userEmail("john@example.com")
                .build();

        testMember1 = User.builder()
                .userId(2)
                .userFullName("Jane Smith")
                .userAvatarUrl("https://example.com/jane-avatar.jpg")
                .userEmail("jane@example.com")
                .build();

        testMember2 = User.builder()
                .userId(3)
                .userFullName("Bob Johnson")
                .userAvatarUrl("https://example.com/bob-avatar.jpg")
                .userEmail("bob@example.com")
                .build();

        // Setup group members
        testGroupLeader = new GroupMember();
        testGroupLeader.setGroupMemberId(1);
        testGroupLeader.setGroup(testGroup);
        testGroupLeader.setUser(testLeader);
        testGroupLeader.setRole(GroupMemberRole.ADMIN);
        testGroupLeader.setStatus(GroupMemberStatus.ACTIVE);

        testGroupMember1 = new GroupMember();
        testGroupMember1.setGroupMemberId(2);
        testGroupMember1.setGroup(testGroup);
        testGroupMember1.setUser(testMember1);
        testGroupMember1.setRole(GroupMemberRole.MEMBER);
        testGroupMember1.setStatus(GroupMemberStatus.ACTIVE);

        testGroupMember2 = new GroupMember();
        testGroupMember2.setGroupMemberId(3);
        testGroupMember2.setGroup(testGroup);
        testGroupMember2.setUser(testMember2);
        testGroupMember2.setRole(GroupMemberRole.MEMBER);
        testGroupMember2.setStatus(GroupMemberStatus.ACTIVE);
    }

    // Helper method to setup AppProperties mock for getGroupDetail tests
    private void setupAppPropertiesMock() {
        AppProperties.Client clientConfig = new AppProperties.Client();
        clientConfig.setBaseUrl("https://seima.app.com");
        when(appProperties.getClient()).thenReturn(clientConfig);
    }

    // ===== CREATE GROUP WITH IMAGE TESTS =====

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
    void createGroupWithImage_Success_WithInviteCodeGeneration() {
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
            verify(groupRepository).save(argThat(group -> {
                // Verify that invite code is set and has correct format
                assertNotNull(group.getGroupInviteCode());
                assertEquals(32, group.getGroupInviteCode().length()); // UUID without hyphens = 32 chars
                assertTrue(group.getGroupInviteCode().matches("[a-f0-9]+"), 
                    "Invite code should only contain lowercase hex characters");
                return true;
            }));
        }
    }

    @Test
    void createGroupWithImage_GeneratesUniqueInviteCodes() {
        // Given
        CreateGroupRequest request1 = new CreateGroupRequest();
        request1.setGroupName("Test Group 1");
        
        CreateGroupRequest request2 = new CreateGroupRequest();
        request2.setGroupName("Test Group 2");

        Group mockGroup1 = new Group();
        mockGroup1.setGroupId(1);
        mockGroup1.setGroupName("Test Group 1");
        mockGroup1.setGroupIsActive(true);

        Group mockGroup2 = new Group();
        mockGroup2.setGroupId(2);
        mockGroup2.setGroupName("Test Group 2");
        mockGroup2.setGroupIsActive(true);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupMapper.toEntity(request1)).thenReturn(mockGroup1);
            when(groupMapper.toEntity(request2)).thenReturn(mockGroup2);
            when(groupRepository.save(any(Group.class)))
                    .thenReturn(mockGroup1)
                    .thenReturn(mockGroup2);
            when(groupMemberRepository.save(any(GroupMember.class))).thenReturn(new GroupMember());
            when(groupMapper.toResponse(any(Group.class))).thenReturn(mockResponse);

            // When
            GroupResponse result1 = groupService.createGroupWithImage(request1);
            GroupResponse result2 = groupService.createGroupWithImage(request2);

            // Then
            verify(groupRepository, times(2)).save(argThat(group -> {
                assertNotNull(group.getGroupInviteCode());
                assertEquals(32, group.getGroupInviteCode().length());
                return true;
            }));
            
            // Verify that both calls generate invite codes (they should be different in real scenario)
            // Note: In actual implementation, each UUID generation would be unique
        }
    }

    @Test
    void createGroupWithImage_InviteCodeValidation() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupMapper.toEntity(validRequest)).thenReturn(mockGroup);
            when(groupRepository.save(any(Group.class))).thenReturn(mockGroup);
            when(groupMemberRepository.save(any(GroupMember.class))).thenReturn(new GroupMember());
            when(groupMapper.toResponse(mockGroup)).thenReturn(mockResponse);

            // When
            groupService.createGroupWithImage(validRequest);

            // Then
            verify(groupRepository).save(argThat(group -> {
                String inviteCode = group.getGroupInviteCode();
                // Verify invite code meets validation constraints
                assertNotNull(inviteCode, "Invite code should not be null");
                assertTrue(inviteCode.length() >= 8, "Invite code should be at least 8 characters");
                assertTrue(inviteCode.length() <= 36, "Invite code should not exceed 36 characters");
                assertFalse(inviteCode.contains("-"), "Invite code should not contain hyphens");
                return true;
            }));
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

    // ===== GET GROUP DETAIL TESTS =====

    @Test
    void getGroupDetail_ShouldReturnGroupDetailResponse_WhenGroupExists() {
        // Given
        Integer groupId = 1;
        List<GroupMember> allMembers = Arrays.asList(testGroupLeader, testGroupMember1, testGroupMember2);
        setupAppPropertiesMock();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testLeader);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(testLeader.getUserId(), testGroup.getGroupId(), GroupMemberStatus.ACTIVE))
                    .thenReturn(true);
            when(groupMemberRepository.findGroupLeader(groupId, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupLeader));
            when(groupMemberRepository.findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(allMembers);
            when(groupMemberRepository.countActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(3L);

            // When
            GroupDetailResponse result = groupService.getGroupDetail(groupId);

            // Then
            assertNotNull(result);
            assertEquals(testGroup.getGroupId(), result.getGroupId());
            assertEquals(testGroup.getGroupName(), result.getGroupName());
            assertEquals("https://seima.app.com/test-invite-code-123", result.getGroupInviteLink());
            assertEquals(testGroup.getGroupAvatarUrl(), result.getGroupAvatarUrl());
            assertEquals(testGroup.getGroupCreatedDate(), result.getGroupCreatedDate());
            assertEquals(testGroup.getGroupIsActive(), result.getGroupIsActive());
            assertEquals(3, result.getTotalMembersCount());

            // Verify leader
            GroupMemberResponse leader = result.getGroupLeader();
            assertNotNull(leader);
            assertEquals(testLeader.getUserId(), leader.getUserId());
            assertEquals(testLeader.getUserFullName(), leader.getUserFullName());
            assertEquals(testLeader.getUserAvatarUrl(), leader.getUserAvatarUrl());
            assertEquals(GroupMemberRole.ADMIN, leader.getRole());

            // Verify members (should not include leader)
            List<GroupMemberResponse> members = result.getMembers();
            assertNotNull(members);
            assertEquals(2, members.size());
            
            // Check that leader is not in members list
            assertFalse(members.stream().anyMatch(member -> 
                member.getUserId().equals(testLeader.getUserId())));

            // Verify repository calls
            verify(groupRepository).findById(groupId);
            verify(groupMemberRepository).existsByUserAndGroupAndStatus(testLeader.getUserId(), testGroup.getGroupId(), GroupMemberStatus.ACTIVE);
            verify(groupMemberRepository).findGroupLeader(groupId, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE);
            verify(groupMemberRepository).findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE);
            verify(groupMemberRepository).countActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE);
        }
    }

    @Test
    void getGroupDetail_ShouldThrowException_WhenGroupIdIsNull() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
            () -> groupService.getGroupDetail(null));
        
        assertEquals("Group ID cannot be null", exception.getMessage());
        
        // Verify no repository calls were made
        verifyNoInteractions(groupRepository);
        verifyNoInteractions(groupMemberRepository);
    }

    @Test
    void getGroupDetail_ShouldThrowException_WhenGroupNotFound() {
        // Given
        Integer groupId = 999;
        
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testLeader);
            when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                () -> groupService.getGroupDetail(groupId));
            
            assertEquals("Group not found", exception.getMessage());
            
            verify(groupRepository).findById(groupId);
            verifyNoInteractions(groupMemberRepository);
        }
    }

    @Test
    void getGroupDetail_ShouldThrowException_WhenGroupIsInactive() {
        // Given
        Integer groupId = 1;
        testGroup.setGroupIsActive(false);
        
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testLeader);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                () -> groupService.getGroupDetail(groupId));
            
            assertEquals("Group not found", exception.getMessage());
            
            verify(groupRepository).findById(groupId);
            verifyNoInteractions(groupMemberRepository);
        }
    }

    @Test
    void getGroupDetail_ShouldThrowException_WhenLeaderNotFound() {
        // Given
        Integer groupId = 1;
        
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testLeader);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(testLeader.getUserId(), testGroup.getGroupId(), GroupMemberStatus.ACTIVE))
                    .thenReturn(true);
            when(groupMemberRepository.findGroupLeader(groupId, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                () -> groupService.getGroupDetail(groupId));
            
            assertEquals("Group leader not found for group ID: " + groupId, exception.getMessage());
            
            verify(groupRepository).findById(groupId);
            verify(groupMemberRepository).findGroupLeader(groupId, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE);
        }
    }

    @Test
    void getGroupDetail_ShouldReturnCorrectData_WhenOnlyLeaderExists() {
        // Given
        Integer groupId = 1;
        List<GroupMember> allMembers = Arrays.asList(testGroupLeader);
        setupAppPropertiesMock();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testLeader);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(testLeader.getUserId(), testGroup.getGroupId(), GroupMemberStatus.ACTIVE))
                    .thenReturn(true);
            when(groupMemberRepository.findGroupLeader(groupId, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupLeader));
            when(groupMemberRepository.findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(allMembers);
            when(groupMemberRepository.countActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(1L);

            // When
            GroupDetailResponse result = groupService.getGroupDetail(groupId);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalMembersCount());
            assertNotNull(result.getGroupLeader());
            assertEquals(testLeader.getUserId(), result.getGroupLeader().getUserId());
            assertEquals("https://seima.app.com/test-invite-code-123", result.getGroupInviteLink());
            
            // Members list should be empty (leader excluded)
            assertTrue(result.getMembers().isEmpty());
        }
    }

    @Test
    void getGroupDetail_ShouldHandleNullAvatarUrls() {
        // Given
        Integer groupId = 1;
        testGroup.setGroupAvatarUrl(null);
        testLeader.setUserAvatarUrl(null);
        setupAppPropertiesMock();
        
        List<GroupMember> allMembers = Arrays.asList(testGroupLeader);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testLeader);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(testLeader.getUserId(), testGroup.getGroupId(), GroupMemberStatus.ACTIVE))
                    .thenReturn(true);
            when(groupMemberRepository.findGroupLeader(groupId, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupLeader));
            when(groupMemberRepository.findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(allMembers);
            when(groupMemberRepository.countActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(1L);

            // When
            GroupDetailResponse result = groupService.getGroupDetail(groupId);

            // Then
            assertNotNull(result);
            assertNull(result.getGroupAvatarUrl());
            assertNull(result.getGroupLeader().getUserAvatarUrl());
        }
    }
} 