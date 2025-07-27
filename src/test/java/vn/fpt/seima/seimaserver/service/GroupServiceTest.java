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
import vn.fpt.seima.seimaserver.dto.response.group.GroupResponse;
import vn.fpt.seima.seimaserver.dto.response.group.UserJoinedGroupResponse;
import vn.fpt.seima.seimaserver.dto.response.group.UserPendingGroupResponse;
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

    @Mock
    private GroupPermissionService groupPermissionService;

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
        mockUser.setUserIsActive(true);

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

        // Setup test users with explicit active status
        testLeader = User.builder()
                .userId(1)
                .userFullName("John Doe")
                .userAvatarUrl("https://example.com/john-avatar.jpg")
                .userEmail("john@example.com")
                .userIsActive(true)
                .build();

        testMember1 = User.builder()
                .userId(2)
                .userFullName("Jane Smith")
                .userAvatarUrl("https://example.com/jane-avatar.jpg")
                .userEmail("jane@example.com")
                .userIsActive(true)
                .build();

        testMember2 = User.builder()
                .userId(3)
                .userFullName("Bob Johnson")
                .userAvatarUrl("https://example.com/bob-avatar.jpg")
                .userEmail("bob@example.com")
                .userIsActive(true)
                .build();

        // Setup group members
        testGroupLeader = new GroupMember();
        testGroupLeader.setGroupMemberId(1);
        testGroupLeader.setGroup(testGroup);
        testGroupLeader.setUser(testLeader);
        testGroupLeader.setRole(GroupMemberRole.OWNER);
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
            when(cloudinaryService.uploadImage(any(MultipartFile.class), eq("groups"))).thenReturn(uploadResult);
            when(groupRepository.save(any(Group.class))).thenReturn(mockGroup);
            when(groupMemberRepository.save(any(GroupMember.class))).thenReturn(new GroupMember());
            when(groupMapper.toResponse(mockGroup)).thenReturn(mockResponse);

            // When
            GroupResponse result = groupService.createGroupWithImage(validRequest);

            // Then
            assertNotNull(result);
            assertEquals("Test Group", result.getGroupName());
            verify(cloudinaryService).uploadImage(any(MultipartFile.class), eq("groups"));
            verify(groupRepository).save(any(Group.class));
            verify(groupMemberRepository).save(any(GroupMember.class));
        }
    }

    @Test
    void createGroupWithImage_ThrowsException_WhenRequestIsNull() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class, () -> 
            groupService.createGroupWithImage(null)
        );
        assertEquals("Group request cannot be null", exception.getMessage());
    }

    @Test
    void createGroupWithImage_ThrowsException_WhenGroupNameIsEmpty() {
        // Given
        validRequest.setGroupName("");

        // When & Then
        GroupException exception = assertThrows(GroupException.class, () -> 
            groupService.createGroupWithImage(validRequest)
        );
        assertEquals("Group name is required and cannot be empty", exception.getMessage());
    }

    @Test
    void createGroupWithImage_ThrowsException_WhenGroupNameTooLong() {
        // Given
        String longName = "a".repeat(101);
        validRequest.setGroupName(longName);

        // When & Then
        GroupException exception = assertThrows(GroupException.class, () -> 
            groupService.createGroupWithImage(validRequest)
        );
        assertEquals("Group name cannot exceed 100 characters", exception.getMessage());
    }

    @Test
    void createGroupWithImage_ThrowsException_WhenUserNotFound() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(null);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, () -> 
                groupService.createGroupWithImage(validRequest)
            );
            assertEquals("Unable to identify the current user", exception.getMessage());
        }
    }

    @Test
    void createGroupWithImage_ThrowsException_WhenImageTooLarge() {
        // Given
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "image", 
                "large.jpg", 
                "image/jpeg", 
                largeContent
        );
        validRequest.setImage(largeFile);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, () -> 
                groupService.createGroupWithImage(validRequest)
            );
            assertEquals("Image file size must be less than 5MB", exception.getMessage());
        }
    }

    // ===== GET GROUP DETAIL TESTS =====

    @Test
    void getGroupDetail_ShouldReturnGroupDetailResponse_WhenGroupExists() {
        // Given
        Integer groupId = 1;
        testLeader.setUserIsActive(true);
        testMember1.setUserIsActive(true);
        testMember2.setUserIsActive(true);

        List<GroupMember> allMembers = Arrays.asList(testGroupLeader, testGroupMember1, testGroupMember2);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testLeader);
            
            setupAppPropertiesMock();
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    testLeader.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupLeader));
            when(groupPermissionService.canViewGroupMembers(GroupMemberRole.OWNER)).thenReturn(true);
            when(groupMemberRepository.findGroupOwner(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupLeader));
            when(groupMemberRepository.findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(allMembers);
            
            GroupDetailResponse expectedResponse = new GroupDetailResponse();
            expectedResponse.setGroupId(groupId);
            expectedResponse.setCurrentUserRole(GroupMemberRole.OWNER);

            // When
            GroupDetailResponse result = groupService.getGroupDetail(groupId);

            // Then
            assertNotNull(result);
            assertEquals(groupId, result.getGroupId());

            verify(groupRepository).findById(groupId);
            verify(groupMemberRepository).findByUserAndGroupAndStatus(testLeader.getUserId(), groupId, GroupMemberStatus.ACTIVE);
            verify(groupPermissionService).canViewGroupMembers(GroupMemberRole.OWNER);
            verify(groupMemberRepository).findGroupOwner(groupId, GroupMemberStatus.ACTIVE);
            verify(groupMemberRepository).findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE);
        }
    }

    @Test
    void getGroupDetail_ShouldThrowException_WhenGroupIdIsNull() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupService.getGroupDetail(null));

        assertEquals("Group ID cannot be null", exception.getMessage());
        verifyNoInteractions(groupRepository);
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
        }
    }

    @Test
    void getGroupDetail_ShouldThrowException_WhenUserNotMember() {
        // Given
        Integer groupId = 1;

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testLeader);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(testLeader.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.getGroupDetail(groupId));

            assertEquals("You don't have permission to view this group", exception.getMessage());
            verify(groupRepository).findById(groupId);
            verify(groupMemberRepository).findByUserAndGroupAndStatus(testLeader.getUserId(), groupId, GroupMemberStatus.ACTIVE);
        }
    }

    @Test
    void getGroupDetail_ShouldThrowException_WhenOwnerNotFound() {
        // Given
        Integer groupId = 1;

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testLeader);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(testLeader.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupLeader));
            when(groupPermissionService.canViewGroupMembers(GroupMemberRole.OWNER)).thenReturn(true);
            when(groupMemberRepository.findGroupOwner(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.getGroupDetail(groupId));

            assertEquals("Group owner not found for group ID: 1", exception.getMessage());

            verify(groupRepository).findById(groupId);
            verify(groupMemberRepository).findGroupOwner(groupId, GroupMemberStatus.ACTIVE);
            verify(groupMemberRepository, never()).findActiveGroupMembers(anyInt(), any());
        }
    }

    // ===== UPDATE GROUP INFORMATION TESTS =====

    @Test
    void updateGroupInformation_Success_WithValidNameUpdate() {
        // Given
        Integer groupId = 1;
        validUpdateRequest.setGroupName("Updated Group Name");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(mockUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupLeader));
            when(groupPermissionService.canUpdateGroupInfo(GroupMemberRole.OWNER)).thenReturn(true);
            when(groupRepository.save(any(Group.class))).thenReturn(mockGroup);
            when(groupMapper.toResponse(mockGroup)).thenReturn(mockResponse);

            // When
            GroupResponse result = groupService.updateGroupInformation(groupId, validUpdateRequest);

            // Then
            assertNotNull(result);
            verify(groupRepository).findById(groupId);
            verify(groupMemberRepository).findByUserAndGroupAndStatus(mockUser.getUserId(), groupId, GroupMemberStatus.ACTIVE);
            verify(groupPermissionService).canUpdateGroupInfo(GroupMemberRole.OWNER);
            verify(groupRepository).save(any(Group.class));
            verify(groupMapper).toResponse(mockGroup);
        }
    }

    @Test
    void updateGroupInformation_Success_WithImageUpdate() {
        // Given
        Integer groupId = 1;
        MockMultipartFile newImage = new MockMultipartFile(
                "image", 
                "new-image.jpg", 
                "image/jpeg", 
                "new image content".getBytes()
        );
        validUpdateRequest.setImage(newImage);
        validUpdateRequest.setGroupName(null); // Only update image

        String uploadedImageUrl = "https://cloudinary.com/new-image.jpg";
        Map<String, Object> uploadResult = Map.of("secure_url", uploadedImageUrl);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(mockUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupLeader));
            when(groupPermissionService.canUpdateGroupInfo(GroupMemberRole.OWNER)).thenReturn(true);
            when(cloudinaryService.uploadImage(any(MultipartFile.class), eq("groups"))).thenReturn(uploadResult);
            when(groupRepository.save(any(Group.class))).thenReturn(mockGroup);
            when(groupMapper.toResponse(mockGroup)).thenReturn(mockResponse);

            // When
            GroupResponse result = groupService.updateGroupInformation(groupId, validUpdateRequest);

            // Then
            assertNotNull(result);
            verify(cloudinaryService).uploadImage(any(MultipartFile.class), eq("groups"));
            verify(groupRepository).save(any(Group.class));
        }
    }

    @Test
    void updateGroupInformation_Success_WithRemoveAvatar() {
        // Given
        Integer groupId = 1;
        validUpdateRequest.setRemoveCurrentAvatar(true);
        validUpdateRequest.setGroupName(null);
        validUpdateRequest.setImage(null);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(mockUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupLeader));
            when(groupPermissionService.canUpdateGroupInfo(GroupMemberRole.OWNER)).thenReturn(true);
            when(groupRepository.save(any(Group.class))).thenReturn(mockGroup);
            when(groupMapper.toResponse(mockGroup)).thenReturn(mockResponse);

            // When
            GroupResponse result = groupService.updateGroupInformation(groupId, validUpdateRequest);

            // Then
            assertNotNull(result);
            verify(groupRepository).save(any(Group.class));
        }
    }

    @Test
    void updateGroupInformation_ThrowsException_WhenGroupIdIsNull() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupService.updateGroupInformation(null, validUpdateRequest));

        assertEquals("Group ID cannot be null", exception.getMessage());
        verifyNoInteractions(groupRepository);
    }

    @Test
    void updateGroupInformation_ThrowsException_WhenRequestIsNull() {
        // Given
        Integer groupId = 1;

        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupService.updateGroupInformation(groupId, null));

        assertEquals("Update request cannot be null", exception.getMessage());
        verifyNoInteractions(groupRepository);
    }

    @Test
    void updateGroupInformation_ThrowsException_WhenNoUpdateData() {
        // Given
        Integer groupId = 1;
        UpdateGroupRequest emptyRequest = new UpdateGroupRequest();

        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupService.updateGroupInformation(groupId, emptyRequest));

        assertEquals("At least one field (groupName or image) must be provided for update", exception.getMessage());
        verifyNoInteractions(groupRepository);
    }

    @Test
    void updateGroupInformation_ThrowsException_WhenGroupNotFound() {
        // Given
        Integer groupId = 999;

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.updateGroupInformation(groupId, validUpdateRequest));

            assertEquals("Group not found", exception.getMessage());
            verify(groupRepository).findById(groupId);
        }
    }

    @Test
    void updateGroupInformation_ThrowsException_WhenUserNotMember() {
        // Given
        Integer groupId = 1;

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(mockUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.updateGroupInformation(groupId, validUpdateRequest));

            assertEquals("You are not a member of this group", exception.getMessage());
            verify(groupRepository).findById(groupId);
            verify(groupMemberRepository).findByUserAndGroupAndStatus(mockUser.getUserId(), groupId, GroupMemberStatus.ACTIVE);
        }
    }

    @Test
    void updateGroupInformation_ThrowsException_WhenUserLacksPermission() {
        // Given
        Integer groupId = 1;

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(mockUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupMember1)); // Member role
            when(groupPermissionService.canUpdateGroupInfo(GroupMemberRole.MEMBER)).thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.updateGroupInformation(groupId, validUpdateRequest));

            assertEquals("Only group owners can update group information", exception.getMessage());
            verify(groupPermissionService).canUpdateGroupInfo(GroupMemberRole.MEMBER);
        }
    }

    @Test
    void updateGroupInformation_ThrowsException_WhenImageAndRemoveAvatarBothProvided() {
        // Given
        Integer groupId = 1;
        MockMultipartFile newImage = new MockMultipartFile(
                "image", 
                "new-image.jpg", 
                "image/jpeg", 
                "new image content".getBytes()
        );
        validUpdateRequest.setImage(newImage);
        validUpdateRequest.setRemoveCurrentAvatar(true);

        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupService.updateGroupInformation(groupId, validUpdateRequest));

        assertEquals("Cannot provide new image and remove current avatar at the same time", exception.getMessage());
        verifyNoInteractions(groupRepository);
    }

    // ===== GET USER JOINED GROUPS TESTS =====

    @Test
    void getUserJoinedGroups_Success_WithMultipleGroups() {
        // Given
        List<GroupMember> userMemberships = Arrays.asList(testGroupLeader, testGroupMember1);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testLeader);
            
            when(groupMemberRepository.findUserJoinedGroups(
                    testLeader.getUserId(), GroupMemberStatus.ACTIVE, true))
                    .thenReturn(userMemberships);
            when(groupMemberRepository.findGroupOwner(anyInt(), eq(GroupMemberStatus.ACTIVE)))
                    .thenReturn(Optional.of(testGroupLeader));
            when(groupMemberRepository.findActiveGroupMembers(anyInt(), eq(GroupMemberStatus.ACTIVE)))
                    .thenReturn(Arrays.asList(testGroupLeader, testGroupMember1, testGroupMember2));

            // When
            List<UserJoinedGroupResponse> result = groupService.getUserJoinedGroups();

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(groupMemberRepository).findUserJoinedGroups(testLeader.getUserId(), GroupMemberStatus.ACTIVE, true);
        }
    }

    @Test
    void getUserJoinedGroups_Success_WithEmptyList() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testLeader);
            
            when(groupMemberRepository.findUserJoinedGroups(
                    testLeader.getUserId(), GroupMemberStatus.ACTIVE, true))
                    .thenReturn(Collections.emptyList());

            // When
            List<UserJoinedGroupResponse> result = groupService.getUserJoinedGroups();

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
            verify(groupMemberRepository).findUserJoinedGroups(testLeader.getUserId(), GroupMemberStatus.ACTIVE, true);
        }
    }

    @Test
    void getUserJoinedGroups_ThrowsException_WhenUserNotFound() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(null);

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.getUserJoinedGroups());

            assertEquals("Unable to identify the current user", exception.getMessage());
            verifyNoInteractions(groupMemberRepository);
        }
    }

    // ===== ARCHIVE GROUP TESTS =====

    @Test
    void archiveGroup_Success_WhenUserIsOwner() {
        // Given
        Integer groupId = 1;

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(mockUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupLeader));
            when(groupPermissionService.canManageGroupSettings(GroupMemberRole.OWNER)).thenReturn(true);
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
            verify(groupMemberRepository).findByUserAndGroupAndStatus(mockUser.getUserId(), groupId, GroupMemberStatus.ACTIVE);
            verify(groupPermissionService).canManageGroupSettings(GroupMemberRole.OWNER);
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
    void archiveGroup_ThrowsException_WhenUserNotMember() {
        // Given
        Integer groupId = 1;

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(mockUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.archiveGroup(groupId));

            assertEquals("You are not a member of this group", exception.getMessage());

            verify(groupRepository).findById(groupId);
            verify(groupMemberRepository).findByUserAndGroupAndStatus(mockUser.getUserId(), groupId, GroupMemberStatus.ACTIVE);
            verify(groupRepository, never()).save(any(Group.class));
        }
    }

    @Test
    void archiveGroup_ThrowsException_WhenUserLacksPermission() {
        // Given
        Integer groupId = 1;

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(mockUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupMember1)); // Member role
            when(groupPermissionService.canManageGroupSettings(GroupMemberRole.MEMBER)).thenReturn(false);
            when(groupPermissionService.getPermissionDescription("ARCHIVE_GROUP", GroupMemberRole.MEMBER, null))
                    .thenReturn("Only group administrators and owners can archive groups");

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.archiveGroup(groupId));

            assertEquals("Only group administrators and owners can archive groups", exception.getMessage());

            verify(groupRepository).findById(groupId);
            verify(groupMemberRepository).findByUserAndGroupAndStatus(mockUser.getUserId(), groupId, GroupMemberStatus.ACTIVE);
            verify(groupPermissionService).canManageGroupSettings(GroupMemberRole.MEMBER);
            verify(groupRepository, never()).save(any(Group.class));
        }
    }

    @Test
    void archiveGroup_ThrowsException_WhenGroupAlreadyArchived() {
        // Given
        Integer groupId = 1;
        Group archivedGroup = new Group();
        archivedGroup.setGroupId(groupId);
        archivedGroup.setGroupIsActive(false); // Already archived

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(archivedGroup));

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.archiveGroup(groupId));

            assertEquals("Group is already archived", exception.getMessage());
            verify(groupRepository).findById(groupId);
            verify(groupRepository, never()).save(any(Group.class));
        }
    }

    // ===== getUserPendingGroups Tests =====
    
    @Test
    void getUserPendingGroups_Success_WithMultiplePendingGroups() {
        // Given
        User currentUser = createTestUserWithId(1, "Test User", "test@example.com");
        
        Group pendingGroup1 = createTestGroupWithId(1, "Pending Group 1", true);
        Group pendingGroup2 = createTestGroupWithId(2, "Pending Group 2", true);
        
        GroupMember pendingMembership1 = createGroupMember(pendingGroup1, currentUser, GroupMemberRole.MEMBER, GroupMemberStatus.PENDING_APPROVAL);
        pendingMembership1.setJoinDate(LocalDateTime.now().minusDays(2));
        
        GroupMember pendingMembership2 = createGroupMember(pendingGroup2, currentUser, GroupMemberRole.MEMBER, GroupMemberStatus.PENDING_APPROVAL);
        pendingMembership2.setJoinDate(LocalDateTime.now().minusDays(1));
        
        List<GroupMember> pendingMemberships = Arrays.asList(pendingMembership1, pendingMembership2);
        
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            when(groupMemberRepository.findUserPendingGroups(currentUser.getUserId(), GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(pendingMemberships);
            when(groupMemberRepository.countActiveGroupMembers(1, GroupMemberStatus.ACTIVE)).thenReturn(5L);
            when(groupMemberRepository.countActiveGroupMembers(2, GroupMemberStatus.ACTIVE)).thenReturn(3L);

            // When
            List<UserPendingGroupResponse> result = groupService.getUserPendingGroups();

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            
            // Verify first group
            UserPendingGroupResponse firstGroup = result.get(0);
            assertEquals(1, firstGroup.getGroupId());
            assertEquals("Pending Group 1", firstGroup.getGroupName());
            assertEquals(5, firstGroup.getActiveMemberCount());
            assertEquals(pendingMembership1.getJoinDate(), firstGroup.getRequestedAt());
            
            // Verify second group
            UserPendingGroupResponse secondGroup = result.get(1);
            assertEquals(2, secondGroup.getGroupId());
            assertEquals("Pending Group 2", secondGroup.getGroupName());
            assertEquals(3, secondGroup.getActiveMemberCount());
            assertEquals(pendingMembership2.getJoinDate(), secondGroup.getRequestedAt());

            verify(groupMemberRepository).findUserPendingGroups(currentUser.getUserId(), GroupMemberStatus.PENDING_APPROVAL);
            verify(groupMemberRepository).countActiveGroupMembers(1, GroupMemberStatus.ACTIVE);
            verify(groupMemberRepository).countActiveGroupMembers(2, GroupMemberStatus.ACTIVE);
        }
    }

    @Test
    void getUserPendingGroups_Success_WithEmptyList() {
        // Given
        User currentUser = createTestUserWithId(1, "Test User", "test@example.com");
        
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            when(groupMemberRepository.findUserPendingGroups(currentUser.getUserId(), GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(Collections.emptyList());

            // When
            List<UserPendingGroupResponse> result = groupService.getUserPendingGroups();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(groupMemberRepository).findUserPendingGroups(currentUser.getUserId(), GroupMemberStatus.PENDING_APPROVAL);
            verify(groupMemberRepository, never()).countActiveGroupMembers(anyInt(), any(GroupMemberStatus.class));
        }
    }

    @Test
    void getUserPendingGroups_ThrowsException_WhenUserNotFound() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(null);

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupService.getUserPendingGroups());

            assertEquals("Unable to identify the current user", exception.getMessage());

            verify(groupMemberRepository, never()).findUserPendingGroups(anyInt(), any(GroupMemberStatus.class));
        }
    }

    // Helper methods
    private User createTestUserWithId(Integer userId, String fullName, String email) {
        User user = new User();
        user.setUserId(userId);
        user.setUserFullName(fullName);
        user.setUserEmail(email);
        user.setUserIsActive(true);
        return user;
    }

    private Group createTestGroupWithId(Integer groupId, String groupName, boolean isActive) {
        Group group = new Group();
        group.setGroupId(groupId);
        group.setGroupName(groupName);
        group.setGroupIsActive(isActive);
        group.setGroupCreatedDate(LocalDateTime.now());
        return group;
    }

    private GroupMember createGroupMember(Group group, User user, GroupMemberRole role, GroupMemberStatus status) {
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setRole(role);
        member.setStatus(status);
        return member;
    }
} 