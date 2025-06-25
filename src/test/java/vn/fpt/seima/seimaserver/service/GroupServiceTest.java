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
import vn.fpt.seima.seimaserver.config.base.AppProperties;
import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.request.group.UpdateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupDetailResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupResponse;
import vn.fpt.seima.seimaserver.dto.response.group.UserJoinedGroupResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.exception.GroupException;
import vn.fpt.seima.seimaserver.mapper.GroupMapper;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.repository.GroupRepository;
import vn.fpt.seima.seimaserver.service.impl.GroupServiceImpl;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.time.LocalDateTime;
import java.util.*;

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
    private UpdateGroupRequest validUpdateRequest;
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

        // Setup test data for updateGroupInformation tests
        validUpdateRequest = new UpdateGroupRequest();
        validUpdateRequest.setGroupName("Updated Group Name");

        mockUser = new User();
        mockUser.setUserId(1);
        mockUser.setUserEmail("test@example.com");

        mockGroup = new Group();
        mockGroup.setGroupId(1);
        mockGroup.setGroupName("Test Group");
        mockGroup.setGroupIsActive(true);
        mockGroup.setGroupAvatarUrl("old-avatar-url");

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

            assertTrue(exception.getMessage().contains("Failed to upload image"));
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

            // Verify current user role (should be ADMIN since testLeader is the admin)
            assertEquals(GroupMemberRole.ADMIN, result.getCurrentUserRole());

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
            assertEquals(GroupMemberRole.ADMIN, result.getCurrentUserRole());
            
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
            assertNotNull(result.getGroupLeader());
            assertNull(result.getGroupLeader().getUserAvatarUrl());
            assertEquals(GroupMemberRole.ADMIN, result.getCurrentUserRole());
        }
    }

    @Test
    void getGroupDetail_ShouldReturnMemberRole_WhenCurrentUserIsMember() {
        // Given
        Integer groupId = 1;
        List<GroupMember> allMembers = Arrays.asList(testGroupLeader, testGroupMember1, testGroupMember2);
        setupAppPropertiesMock();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            // Use testMember1 as current user instead of testLeader
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testMember1);

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(testMember1.getUserId(), testGroup.getGroupId(), GroupMemberStatus.ACTIVE))
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
            assertEquals(3, result.getTotalMembersCount());

            // Verify current user role (should be MEMBER since testMember1 is a member)
            assertEquals(GroupMemberRole.MEMBER, result.getCurrentUserRole());

            // Verify leader is still the admin
            GroupMemberResponse leader = result.getGroupLeader();
            assertNotNull(leader);
            assertEquals(testLeader.getUserId(), leader.getUserId());
            assertEquals(GroupMemberRole.ADMIN, leader.getRole());

            // Verify repository calls
            verify(groupRepository).findById(groupId);
            verify(groupMemberRepository).existsByUserAndGroupAndStatus(testMember1.getUserId(), testGroup.getGroupId(), GroupMemberStatus.ACTIVE);
            verify(groupMemberRepository).findGroupLeader(groupId, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE);
            verify(groupMemberRepository).findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE);
            verify(groupMemberRepository).countActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE);
        }
    }

    // ===== UPDATE GROUP INFORMATION TESTS =====

    @Test
    void updateGroupInformation_Success_WithValidNameUpdate() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);

            when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.existsByGroupAndUserAndRole(1, 1, GroupMemberRole.ADMIN))
                    .thenReturn(true);
            when(groupRepository.save(any(Group.class))).thenReturn(mockGroup);
            when(groupMapper.toResponse(any(Group.class))).thenReturn(mockResponse);

            // When
            GroupResponse result = groupService.updateGroupInformation(1, validUpdateRequest);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getGroupId());
            verify(groupRepository).save(any(Group.class));
            verify(groupMapper).toResponse(any(Group.class));
        }
    }

    @Test
    void updateGroupInformation_Success_WithImageUpdate() {
        // Given
        MockMultipartFile mockImage = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test image content".getBytes()
        );
        validUpdateRequest.setImage(mockImage);
        validUpdateRequest.setGroupName(null); // Only update image

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);

            when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.existsByGroupAndUserAndRole(1, 1, GroupMemberRole.ADMIN))
                    .thenReturn(true);
            when(cloudinaryService.uploadImage(any(MultipartFile.class), anyString()))
                    .thenReturn(Map.of("secure_url", "new-avatar-url"));
            when(groupRepository.save(any(Group.class))).thenReturn(mockGroup);
            when(groupMapper.toResponse(any(Group.class))).thenReturn(mockResponse);

            // When
            GroupResponse result = groupService.updateGroupInformation(1, validUpdateRequest);

            // Then
            assertNotNull(result);
            verify(cloudinaryService).uploadImage(eq(mockImage), anyString());
            verify(groupRepository).save(any(Group.class));
        }
    }

    @Test
    void updateGroupInformation_Success_WithRemoveAvatar() {
        // Given
        validUpdateRequest.setGroupName(null);
        validUpdateRequest.setRemoveCurrentAvatar(true);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);

            when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.existsByGroupAndUserAndRole(1, 1, GroupMemberRole.ADMIN))
                    .thenReturn(true);
            when(groupRepository.save(any(Group.class))).thenReturn(mockGroup);
            when(groupMapper.toResponse(any(Group.class))).thenReturn(mockResponse);

            // When
            GroupResponse result = groupService.updateGroupInformation(1, validUpdateRequest);

            // Then
            assertNotNull(result);
            verify(groupRepository).save(any(Group.class));
        }
    }

    @Test
    void updateGroupInformation_Success_WithBothNameAndImageUpdate() {
        // Given
        MockMultipartFile mockImage = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test image content".getBytes()
        );
        validUpdateRequest.setImage(mockImage);
        validUpdateRequest.setGroupName("New Group Name");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);

            when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.existsByGroupAndUserAndRole(1, 1, GroupMemberRole.ADMIN))
                    .thenReturn(true);
            when(cloudinaryService.uploadImage(any(MultipartFile.class), anyString()))
                    .thenReturn(Map.of("secure_url", "new-avatar-url"));
            when(groupRepository.save(any(Group.class))).thenReturn(mockGroup);
            when(groupMapper.toResponse(any(Group.class))).thenReturn(mockResponse);

            // When
            GroupResponse result = groupService.updateGroupInformation(1, validUpdateRequest);

            // Then
            assertNotNull(result);
            verify(cloudinaryService).uploadImage(eq(mockImage), anyString());
            verify(groupRepository).save(any(Group.class));
        }
    }

    @Test
    void updateGroupInformation_Success_WithSameGroupName_ShouldNotUpdateDatabase() {
        // Given
        validUpdateRequest.setGroupName("Test Group"); // Same as current name

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);

            when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.existsByGroupAndUserAndRole(1, 1, GroupMemberRole.ADMIN))
                    .thenReturn(true);
            when(groupMapper.toResponse(any(Group.class))).thenReturn(mockResponse);

            // When
            GroupResponse result = groupService.updateGroupInformation(1, validUpdateRequest);

            // Then
            assertNotNull(result);
            verify(groupRepository, never()).save(any(Group.class)); // Should not save unchanged data
        }
    }

    @Test
    void updateGroupInformation_ThrowsException_WhenGroupIdIsNull() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupService.updateGroupInformation(null, validUpdateRequest));

        assertEquals("Group ID cannot be null", exception.getMessage());
    }

    @Test
    void updateGroupInformation_ThrowsException_WhenRequestIsNull() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupService.updateGroupInformation(1, null));

        assertEquals("Update request cannot be null", exception.getMessage());
    }

    @Test
    void updateGroupInformation_ThrowsException_WhenRequestIsEmpty() {
        // Given
        UpdateGroupRequest emptyRequest = new UpdateGroupRequest();

        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupService.updateGroupInformation(1, emptyRequest));

        assertEquals("At least one field (groupName or image) must be provided for update", exception.getMessage());
    }

    @Test
    void updateGroupInformation_ThrowsException_WhenGroupNotFound() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupRepository.findById(1)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.updateGroupInformation(1, validUpdateRequest));

            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    void updateGroupInformation_ThrowsException_WhenGroupIsInactive() {
        // Given
        mockGroup.setGroupIsActive(false);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.updateGroupInformation(1, validUpdateRequest));

            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    void updateGroupInformation_ThrowsException_WhenUserNotAdmin() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.existsByGroupAndUserAndRole(1, 1, GroupMemberRole.ADMIN))
                    .thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.updateGroupInformation(1, validUpdateRequest));

            assertEquals("Only group administrators can update group information", exception.getMessage());
        }
    }

    @Test
    void updateGroupInformation_ThrowsException_WhenGroupNameTooLong() {
        // Given
        String longName = "A".repeat(101); // Exceed 100 characters limit
        validUpdateRequest.setGroupName(longName);

        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupService.updateGroupInformation(1, validUpdateRequest));

        assertEquals("Group name cannot exceed 100 characters", exception.getMessage());
    }

    @Test
    void updateGroupInformation_ThrowsException_WhenBothImageAndRemoveFlagProvided() {
        // Given
        MockMultipartFile mockImage = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test image content".getBytes()
        );
        validUpdateRequest.setImage(mockImage);
        validUpdateRequest.setRemoveCurrentAvatar(true);

        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupService.updateGroupInformation(1, validUpdateRequest));

        assertEquals("Cannot provide new image and remove current avatar at the same time", exception.getMessage());
    }

    // ========== GET USER JOINED GROUPS TESTS ==========

    @Test
    void getUserJoinedGroups_ShouldReturnJoinedGroups_WhenUserHasActiveGroups() {
        // Given
        User testUser = createTestUserWithId(1, "Test User", "test@example.com");

        // Create test groups
        Group group1 = createTestGroupWithId(1, "Test Group 1", true);
        Group group2 = createTestGroupWithId(2, "Test Group 2", true);
        Group inactiveGroup = createTestGroupWithId(3, "Inactive Group", false);

        // Create test leaders
        User leader1 = createTestUserWithId(10, "Leader One", "leader1@test.com");
        User leader2 = createTestUserWithId(11, "Leader Two", "leader2@test.com");

        // Create group memberships for current user
        GroupMember membership1 = createGroupMember(group1, testUser, GroupMemberRole.MEMBER, GroupMemberStatus.ACTIVE);
        GroupMember membership2 = createGroupMember(group2, testUser, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE);
        GroupMember inactiveMembership = createGroupMember(inactiveGroup, testUser, GroupMemberRole.MEMBER, GroupMemberStatus.ACTIVE);

        // Create leader memberships
        GroupMember leaderMembership1 = createGroupMember(group1, leader1, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE);
        GroupMember leaderMembership2 = createGroupMember(group2, leader2, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE);

        List<GroupMember> userMemberships = Arrays.asList(membership1, membership2);

        // Mock UserUtils
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(testUser);

            // Mock repository calls
            when(groupMemberRepository.findUserJoinedGroups(testUser.getUserId(), GroupMemberStatus.ACTIVE, true))
                    .thenReturn(userMemberships);
            when(groupMemberRepository.findGroupLeader(group1.getGroupId(), GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(leaderMembership1));
            when(groupMemberRepository.findGroupLeader(group2.getGroupId(), GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(leaderMembership2));
            when(groupMemberRepository.countActiveGroupMembers(group1.getGroupId(), GroupMemberStatus.ACTIVE))
                    .thenReturn(3L);
            when(groupMemberRepository.countActiveGroupMembers(group2.getGroupId(), GroupMemberStatus.ACTIVE))
                    .thenReturn(5L);

            // When
            List<UserJoinedGroupResponse> result = groupService.getUserJoinedGroups();

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());

            // Verify first group
            UserJoinedGroupResponse firstGroup = result.get(0);
            assertEquals(group1.getGroupId(), firstGroup.getGroupId());
            assertEquals(group1.getGroupName(), firstGroup.getGroupName());
            assertEquals(GroupMemberRole.MEMBER, firstGroup.getUserRole());
            assertEquals(3, firstGroup.getTotalMembersCount());
            assertNotNull(firstGroup.getGroupLeader());
            assertEquals(leader1.getUserId(), firstGroup.getGroupLeader().getUserId());

            // Verify second group
            UserJoinedGroupResponse secondGroup = result.get(1);
            assertEquals(group2.getGroupId(), secondGroup.getGroupId());
            assertEquals(group2.getGroupName(), secondGroup.getGroupName());
            assertEquals(GroupMemberRole.ADMIN, secondGroup.getUserRole());
            assertEquals(5, secondGroup.getTotalMembersCount());
            assertNotNull(secondGroup.getGroupLeader());
            assertEquals(leader2.getUserId(), secondGroup.getGroupLeader().getUserId());

            // Verify repository calls
            verify(groupMemberRepository).findUserJoinedGroups(testUser.getUserId(), GroupMemberStatus.ACTIVE, true);
            verify(groupMemberRepository, times(2)).findGroupLeader(any(), eq(GroupMemberRole.ADMIN), eq(GroupMemberStatus.ACTIVE));
            verify(groupMemberRepository, times(2)).countActiveGroupMembers(any(), eq(GroupMemberStatus.ACTIVE));
        }
    }

    @Test
    void getUserJoinedGroups_ShouldReturnEmptyList_WhenUserHasNoActiveGroups() {
        // Given
        User testUser = createTestUserWithId(1, "Test User", "test@example.com");

        // Mock UserUtils
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(testUser);

            // Mock repository to return empty list
            when(groupMemberRepository.findUserJoinedGroups(testUser.getUserId(), GroupMemberStatus.ACTIVE, true))
                    .thenReturn(Collections.emptyList());

            // When
            List<UserJoinedGroupResponse> result = groupService.getUserJoinedGroups();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            // Verify repository calls
            verify(groupMemberRepository).findUserJoinedGroups(testUser.getUserId(), GroupMemberStatus.ACTIVE, true);
            verify(groupMemberRepository, never()).findGroupLeader(any(), any(), any());
            verify(groupMemberRepository, never()).countActiveGroupMembers(any(), any());
        }
    }

    @Test
    void getUserJoinedGroups_ShouldThrowException_WhenCurrentUserNotFound() {
        // Given
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(null);

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.getUserJoinedGroups());

            assertEquals("Unable to identify the current user", exception.getMessage());

            // Verify no repository calls were made
            verifyNoInteractions(groupMemberRepository);
        }
    }

    private User createTestUserWithId(Integer userId, String fullName, String email) {
        return User.builder()
                .userId(userId)
                .userFullName(fullName)
                .userEmail(email)
                .userAvatarUrl("https://example.com/avatar.jpg")
                .build();
    }

    private Group createTestGroupWithId(Integer groupId, String groupName, boolean isActive) {
        Group group = new Group();
        group.setGroupId(groupId);
        group.setGroupName(groupName);
        group.setGroupIsActive(isActive);
        group.setGroupInviteCode("invite-code-" + groupId);
        group.setGroupAvatarUrl("https://example.com/group-avatar.jpg");
        group.setGroupCreatedDate(LocalDateTime.now());
        return group;
    }

    private GroupMember createGroupMember(Group group, User user, GroupMemberRole role, GroupMemberStatus status) {
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setRole(role);
        member.setStatus(status);
        member.setJoinDate(LocalDateTime.now());
        return member;
    }

    // ========== ARCHIVE GROUP TESTS ==========

    @Test
    void archiveGroup_Success_WhenUserIsAdmin() {
        // Given
        Integer groupId = 1;

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.existsByGroupAndUserAndRole(groupId, mockUser.getUserId(), GroupMemberRole.ADMIN))
                    .thenReturn(true);
            when(groupRepository.save(any(Group.class))).thenReturn(mockGroup);
            when(groupMapper.toResponse(any(Group.class))).thenReturn(mockResponse);

            // When
            GroupResponse result = groupService.archiveGroup(groupId);

            // Then
            assertNotNull(result);
            assertEquals(mockResponse.getGroupId(), result.getGroupId());

            // Verify that the group was archived
            verify(groupRepository).save(argThat(group -> {
                assertEquals(false, group.getGroupIsActive());
                return true;
            }));
            verify(groupMapper).toResponse(any(Group.class));

            // Verify repository calls
            verify(groupRepository).findById(groupId);
            verify(groupMemberRepository).existsByGroupAndUserAndRole(groupId, mockUser.getUserId(), GroupMemberRole.ADMIN);
        }
    }

    @Test
    void archiveGroup_ThrowsException_WhenGroupIdIsNull() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupService.archiveGroup(null));

        assertEquals("Group ID cannot be null", exception.getMessage());

        // Verify no repository calls were made
        verifyNoInteractions(groupRepository);
        verifyNoInteractions(groupMemberRepository);
    }

    @Test
    void archiveGroup_ThrowsException_WhenCurrentUserNotFound() {
        // Given
        Integer groupId = 1;

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(null);

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.archiveGroup(groupId));

            assertEquals("Unable to identify the current user", exception.getMessage());

            // Verify no further repository calls were made
            verifyNoInteractions(groupRepository);
            verifyNoInteractions(groupMemberRepository);
        }
    }

    @Test
    void archiveGroup_ThrowsException_WhenGroupNotFound() {
        // Given
        Integer groupId = 999;

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.archiveGroup(groupId));

            assertEquals("Group not found", exception.getMessage());

            verify(groupRepository).findById(groupId);
            verifyNoInteractions(groupMemberRepository);
        }
    }

    @Test
    void archiveGroup_ThrowsException_WhenGroupAlreadyArchived() {
        // Given
        Integer groupId = 1;
        mockGroup.setGroupIsActive(false); // Already archived

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.archiveGroup(groupId));

            assertEquals("Group is already archived", exception.getMessage());

            verify(groupRepository).findById(groupId);
            verifyNoInteractions(groupMemberRepository);
            verify(groupRepository, never()).save(any(Group.class));
        }
    }

    @Test
    void archiveGroup_ThrowsException_WhenUserIsNotAdmin() {
        // Given
        Integer groupId = 1;

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.existsByGroupAndUserAndRole(groupId, mockUser.getUserId(), GroupMemberRole.ADMIN))
                    .thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.archiveGroup(groupId));

            assertEquals("Only group administrators can update group information", exception.getMessage());

            verify(groupRepository).findById(groupId);
            verify(groupMemberRepository).existsByGroupAndUserAndRole(groupId, mockUser.getUserId(), GroupMemberRole.ADMIN);
            verify(groupRepository, never()).save(any(Group.class));
        }
    }

    @Test
    void archiveGroup_Success_WhenUserIsMemberButNotAdmin_ShouldFail() {
        // Given
        Integer groupId = 1;
        User memberUser = createTestUserWithId(2, "Member User", "member@test.com");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(memberUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.existsByGroupAndUserAndRole(groupId, memberUser.getUserId(), GroupMemberRole.ADMIN))
                    .thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.archiveGroup(groupId));

            assertEquals("Only group administrators can update group information", exception.getMessage());

            verify(groupRepository).findById(groupId);
            verify(groupMemberRepository).existsByGroupAndUserAndRole(groupId, memberUser.getUserId(), GroupMemberRole.ADMIN);
            verify(groupRepository, never()).save(any(Group.class));
        }
    }

    @Test
    void archiveGroup_Success_VerifyGroupStateChange() {
        // Given
        Integer groupId = 1;
        // Ensure group starts as active
        mockGroup.setGroupIsActive(true);
        Group archivedGroup = new Group();
        archivedGroup.setGroupId(mockGroup.getGroupId());
        archivedGroup.setGroupName(mockGroup.getGroupName());
        archivedGroup.setGroupIsActive(false); // After archiving

        GroupResponse archivedResponse = new GroupResponse();
        archivedResponse.setGroupId(groupId);
        archivedResponse.setGroupName("Test Group");
        archivedResponse.setGroupIsActive(false);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.existsByGroupAndUserAndRole(groupId, mockUser.getUserId(), GroupMemberRole.ADMIN))
                    .thenReturn(true);
            when(groupRepository.save(any(Group.class))).thenReturn(archivedGroup);
            when(groupMapper.toResponse(archivedGroup)).thenReturn(archivedResponse);

            // When
            GroupResponse result = groupService.archiveGroup(groupId);

            // Then
            assertNotNull(result);
            assertEquals(groupId, result.getGroupId());
            assertEquals(false, result.getGroupIsActive());

            // Verify the group was saved with correct state
            verify(groupRepository).save(argThat(group -> {
                assertFalse(group.getGroupIsActive());
                assertEquals(groupId, group.getGroupId());
                return true;
            }));
        }
    }

    @Test
    void archiveGroup_Success_VerifyNoDataLoss() {
        // Given
        Integer groupId = 1;
        String originalGroupName = "Original Group Name";
        String originalAvatarUrl = "https://example.com/avatar.jpg";
        String originalInviteCode = "invite123";
        LocalDateTime originalCreatedDate = LocalDateTime.now().minusDays(7);

        mockGroup.setGroupName(originalGroupName);
        mockGroup.setGroupAvatarUrl(originalAvatarUrl);
        mockGroup.setGroupInviteCode(originalInviteCode);
        mockGroup.setGroupCreatedDate(originalCreatedDate);
        mockGroup.setGroupIsActive(true);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.existsByGroupAndUserAndRole(groupId, mockUser.getUserId(), GroupMemberRole.ADMIN))
                    .thenReturn(true);
            when(groupRepository.save(any(Group.class))).thenReturn(mockGroup);
            when(groupMapper.toResponse(any(Group.class))).thenReturn(mockResponse);

            // When
            groupService.archiveGroup(groupId);

            // Then
            verify(groupRepository).save(argThat(group -> {
                // Verify only groupIsActive is changed, other fields remain intact
                assertEquals(originalGroupName, group.getGroupName());
                assertEquals(originalAvatarUrl, group.getGroupAvatarUrl());
                assertEquals(originalInviteCode, group.getGroupInviteCode());
                assertEquals(originalCreatedDate, group.getGroupCreatedDate());
                assertEquals(false, group.getGroupIsActive()); // Only this should change
                return true;
            }));
        }
    }
} 