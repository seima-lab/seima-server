package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.fpt.seima.seimaserver.config.base.AppProperties;
import vn.fpt.seima.seimaserver.dto.request.group.CancelJoinGroupRequest;
import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.request.group.UpdateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.*;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.exception.GroupException;
import vn.fpt.seima.seimaserver.mapper.GroupMapper;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.repository.GroupRepository;
import vn.fpt.seima.seimaserver.service.impl.GroupServiceImpl;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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
    private GroupPermissionService groupPermissionService;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private AppProperties appProperties;

    @Mock
    private GroupMapper groupMapper;

    @Mock
    private GroupValidationService groupValidationService;

    @InjectMocks
    private GroupServiceImpl groupService;

    private User testUser;
    private Group testGroup;
    private GroupMember testGroupMember;
    private CreateGroupRequest createGroupRequest;
    private UpdateGroupRequest updateGroupRequest;
    private CancelJoinGroupRequest cancelJoinGroupRequest;

    // Test constants
    private static final Integer TEST_GROUP_ID = 1;
    private static final Integer TEST_USER_ID = 100;
    private static final String TEST_GROUP_NAME = "Test Group";
    private static final String TEST_USER_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = User.builder()
                .userId(TEST_USER_ID)
                .userEmail(TEST_USER_EMAIL)
                .userFullName("Test User")
                .userIsActive(true)
                .build();

        // Setup test group
        testGroup = new Group();
        testGroup.setGroupId(TEST_GROUP_ID);
        testGroup.setGroupName(TEST_GROUP_NAME);
        testGroup.setGroupIsActive(true);
        testGroup.setGroupAvatarUrl("https://example.com/avatar.jpg");

        // Setup test group member
        testGroupMember = new GroupMember();
        testGroupMember.setUser(testUser);
        testGroupMember.setGroup(testGroup);
        testGroupMember.setRole(GroupMemberRole.OWNER);
        testGroupMember.setStatus(GroupMemberStatus.ACTIVE);

        // Setup test requests
        createGroupRequest = CreateGroupRequest.builder()
                .groupName(TEST_GROUP_NAME)
                .build();

        updateGroupRequest = UpdateGroupRequest.builder()
                .groupName("Updated Group Name")
                .build();

        cancelJoinGroupRequest = CancelJoinGroupRequest.builder()
                .groupId(TEST_GROUP_ID)
                .build();
    }

    // ================= CREATE GROUP TESTS =================

    @Test
    void createGroupWithImage_WhenValidInputs_ShouldCreateGroupSuccessfully() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            // Mock the group that will be created by mapper
            Group mappedGroup = new Group();
            mappedGroup.setGroupName(TEST_GROUP_NAME);
            mappedGroup.setGroupIsActive(true);
            
            // Mock the group that will be saved and returned
            Group savedGroup = new Group();
            savedGroup.setGroupId(TEST_GROUP_ID);
            savedGroup.setGroupName(TEST_GROUP_NAME);
            savedGroup.setGroupIsActive(true);
            savedGroup.setGroupAvatarUrl("https://cdn.pixabay.com/photo/2016/11/14/17/39/group-1824145_1280.png");
            
            when(groupMapper.toEntity(createGroupRequest)).thenReturn(mappedGroup);
            when(groupRepository.save(any(Group.class))).thenReturn(savedGroup);
            when(groupMapper.toResponse(any(Group.class))).thenReturn(new GroupResponse());
            
            // Mock validation service to not throw exception
            doNothing().when(groupValidationService).validateUserCanJoinMoreGroups(testUser.getUserId());

            // When
            GroupResponse result = groupService.createGroupWithImage(createGroupRequest);

            // Then
            assertNotNull(result);
            verify(groupRepository).save(any(Group.class));
            verify(groupMapper).toResponse(any(Group.class));
            verify(groupValidationService).validateUserCanJoinMoreGroups(testUser.getUserId());
        }
    }

    @Test
    void createGroupWithImage_WhenRequestIsNull_ShouldThrowGroupException() {
        // When & Then
        GroupException exception = assertThrows(
                GroupException.class,
                () -> groupService.createGroupWithImage(null)
        );

        assertEquals("Group request cannot be null", exception.getMessage());
        verify(groupRepository, never()).save(any());
    }

    @Test
    void createGroupWithImage_WhenUserHasMaxGroups_ShouldThrowGroupException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            // Mock validation service to throw exception
            doThrow(new GroupException("User has reached the maximum number of groups (10). Cannot join more groups."))
                    .when(groupValidationService).validateUserCanJoinMoreGroups(testUser.getUserId());

            // When & Then
            GroupException exception = assertThrows(
                    GroupException.class,
                    () -> groupService.createGroupWithImage(createGroupRequest)
            );

            assertEquals("User has reached the maximum number of groups (10). Cannot join more groups.", 
                    exception.getMessage());
            verify(groupValidationService).validateUserCanJoinMoreGroups(testUser.getUserId());
            verify(groupRepository, never()).save(any());
        }
    }

    @Test
    void createGroupWithImage_WhenGroupNameIsNull_ShouldThrowGroupException() {
        // Given
        createGroupRequest.setGroupName(null);

        // When & Then
        GroupException exception = assertThrows(
                GroupException.class,
                () -> groupService.createGroupWithImage(createGroupRequest)
        );

        assertEquals("Group name is required and cannot be empty", exception.getMessage());
        verify(groupRepository, never()).save(any());
    }

    @Test
    void createGroupWithImage_WhenGroupNameIsEmpty_ShouldThrowGroupException() {
        // Given
        createGroupRequest.setGroupName("");

        // When & Then
        GroupException exception = assertThrows(
                GroupException.class,
                () -> groupService.createGroupWithImage(createGroupRequest)
        );

        assertEquals("Group name is required and cannot be empty", exception.getMessage());
        verify(groupRepository, never()).save(any());
    }

    @Test
    void createGroupWithImage_WhenGroupNameHasWhitespace_ShouldCreateWithWhitespace() {
        // Given
        createGroupRequest.setGroupName("  Test Group  ");

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            // Mock the group that will be created by mapper
            Group mappedGroup = new Group();
            mappedGroup.setGroupName("  Test Group  ");
            mappedGroup.setGroupIsActive(true);
            
            // Mock the group that will be saved and returned
            Group savedGroup = new Group();
            savedGroup.setGroupId(TEST_GROUP_ID);
            savedGroup.setGroupName("  Test Group  ");
            savedGroup.setGroupIsActive(true);
            savedGroup.setGroupAvatarUrl("https://cdn.pixabay.com/photo/2016/11/14/17/39/group-1824145_1280.png");
            
            when(groupMapper.toEntity(createGroupRequest)).thenReturn(mappedGroup);
            when(groupRepository.save(any(Group.class))).thenReturn(savedGroup);
            when(groupMapper.toResponse(any(Group.class))).thenReturn(new GroupResponse());

            // When
            GroupResponse result = groupService.createGroupWithImage(createGroupRequest);

            // Then
            assertNotNull(result);
            verify(groupRepository).save(argThat(group -> 
                group.getGroupName().equals("  Test Group  ")
            ));
        }
    }

    // ================= GET GROUP DETAIL TESTS =================

    @Test
    void getGroupDetail_WhenValidGroupId_ShouldReturnGroupDetail() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupMember));
            when(groupPermissionService.canViewGroupMembers(GroupMemberRole.OWNER)).thenReturn(true);
            when(groupMemberRepository.findGroupOwner(TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupMember));
            when(groupMemberRepository.findActiveGroupMembers(TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(Arrays.asList(testGroupMember));

            // When
            GroupDetailResponse result = groupService.getGroupDetail(TEST_GROUP_ID);

            // Then
            assertNotNull(result);
            verify(groupRepository).findById(TEST_GROUP_ID);
        }
    }

    @Test
    void getGroupDetail_WhenGroupIdIsNull_ShouldThrowGroupException() {
        // When & Then
        GroupException exception = assertThrows(
                GroupException.class,
                () -> groupService.getGroupDetail(null)
        );

        assertEquals("Group ID cannot be null", exception.getMessage());
        verify(groupRepository, never()).findById(any());
    }

    @Test
    void getGroupDetail_WhenGroupNotFound_ShouldThrowGroupException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(
                    GroupException.class,
                    () -> groupService.getGroupDetail(TEST_GROUP_ID)
            );

            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    void getGroupDetail_WhenGroupIsInactive_ShouldThrowGroupException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            testGroup.setGroupIsActive(false);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(testGroup));

            // When & Then
            GroupException exception = assertThrows(
                    GroupException.class,
                    () -> groupService.getGroupDetail(TEST_GROUP_ID)
            );

            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    void getGroupDetail_WhenUserNotMember_ShouldThrowGroupException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(
                    GroupException.class,
                    () -> groupService.getGroupDetail(TEST_GROUP_ID)
            );

            assertEquals("You don't have permission to view this group", exception.getMessage());
        }
    }

    // ================= UPDATE GROUP TESTS =================

    @Test
    void updateGroupInformation_WhenValidInputs_ShouldUpdateGroupSuccessfully() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupMember));
            when(groupPermissionService.canUpdateGroupInfo(GroupMemberRole.OWNER)).thenReturn(true);
            when(groupRepository.save(any(Group.class))).thenReturn(testGroup);
            when(groupMapper.toResponse(any(Group.class))).thenReturn(new GroupResponse());

            // When
            GroupResponse result = groupService.updateGroupInformation(TEST_GROUP_ID, updateGroupRequest);

            // Then
            assertNotNull(result);
            verify(groupRepository).save(any(Group.class));
        }
    }

    @Test
    void updateGroupInformation_WhenGroupIdIsNull_ShouldThrowGroupException() {
        // When & Then
        GroupException exception = assertThrows(
                GroupException.class,
                () -> groupService.updateGroupInformation(null, updateGroupRequest)
        );

        assertEquals("Group ID cannot be null", exception.getMessage());
        verify(groupRepository, never()).findById(any());
    }

    @Test
    void updateGroupInformation_WhenRequestIsNull_ShouldThrowGroupException() {
        // When & Then
        GroupException exception = assertThrows(
                GroupException.class,
                () -> groupService.updateGroupInformation(TEST_GROUP_ID, null)
        );

        assertEquals("Update request cannot be null", exception.getMessage());
        verify(groupRepository, never()).findById(any());
    }

    @Test
    void updateGroupInformation_WhenGroupNotFound_ShouldThrowGroupException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(
                    GroupException.class,
                    () -> groupService.updateGroupInformation(TEST_GROUP_ID, updateGroupRequest)
            );

            assertEquals("Group not found", exception.getMessage());
        }
    }

    // ================= GET USER JOINED GROUPS TESTS =================

    @Test
    void getUserJoinedGroups_WhenValidUser_ShouldReturnJoinedGroups() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            when(groupMemberRepository.findUserJoinedGroups(TEST_USER_ID, GroupMemberStatus.ACTIVE, true))
                    .thenReturn(Arrays.asList(testGroupMember));

            // When
            List<UserJoinedGroupResponse> result = groupService.getUserJoinedGroups();

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Test
    void getUserJoinedGroups_WhenNoJoinedGroups_ShouldReturnEmptyList() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            when(groupMemberRepository.findUserJoinedGroups(TEST_USER_ID, GroupMemberStatus.ACTIVE, true))
                    .thenReturn(Arrays.asList());

            // When
            List<UserJoinedGroupResponse> result = groupService.getUserJoinedGroups();

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    // ================= ARCHIVE GROUP TESTS =================

    @Test
    void archiveGroup_WhenValidGroupId_ShouldArchiveGroupSuccessfully() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupMember));
            when(groupPermissionService.canManageGroupSettings(GroupMemberRole.OWNER)).thenReturn(true);
            when(groupRepository.save(any(Group.class))).thenReturn(testGroup);
            when(groupMapper.toResponse(any(Group.class))).thenReturn(new GroupResponse());

            // When
            GroupResponse result = groupService.archiveGroup(TEST_GROUP_ID);

            // Then
            assertNotNull(result);
            verify(groupRepository).save(any(Group.class));
        }
    }

    @Test
    void archiveGroup_WhenGroupIdIsNull_ShouldThrowGroupException() {
        // When & Then
        GroupException exception = assertThrows(
                GroupException.class,
                () -> groupService.archiveGroup(null)
        );

        assertEquals("Group ID cannot be null", exception.getMessage());
        verify(groupRepository, never()).findById(any());
    }

    @Test
    void archiveGroup_WhenGroupNotFound_ShouldThrowGroupException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(
                    GroupException.class,
                    () -> groupService.archiveGroup(TEST_GROUP_ID)
            );

            assertEquals("Group not found", exception.getMessage());
        }
    }

    // ================= GET CURRENT USER GROUP STATUS TESTS =================

    @Test
    void getCurrentUserGroupStatus_WhenValidGroupId_ShouldReturnStatus() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(TEST_USER_ID, TEST_GROUP_ID))
                    .thenReturn(Optional.of(testGroupMember));

            // When
            GroupMemberStatusResponse result = groupService.getCurrentUserGroupStatus(TEST_GROUP_ID);

            // Then
            assertNotNull(result);
            assertEquals(TEST_GROUP_ID, result.getGroupId());
            assertEquals(GroupMemberStatus.ACTIVE, result.getStatus());
        }
    }

    @Test
    void getCurrentUserGroupStatus_WhenGroupIdIsNull_ShouldThrowGroupException() {
        // When & Then
        GroupException exception = assertThrows(
                GroupException.class,
                () -> groupService.getCurrentUserGroupStatus(null)
        );

        assertEquals("Group ID cannot be null", exception.getMessage());
    }

    @Test
    void getCurrentUserGroupStatus_WhenGroupNotFound_ShouldReturnGroupNotExists() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.empty());

            // When
            GroupMemberStatusResponse result = groupService.getCurrentUserGroupStatus(TEST_GROUP_ID);

            // Then
            assertNotNull(result);
            assertFalse(result.isGroupExists());
            assertNull(result.getStatus());
        }
    }

    @Test
    void getCurrentUserGroupStatus_WhenGroupInactive_ShouldReturnGroupNotExists() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            testGroup.setGroupIsActive(false);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(testGroup));

            // When
            GroupMemberStatusResponse result = groupService.getCurrentUserGroupStatus(TEST_GROUP_ID);

            // Then
            assertNotNull(result);
            assertFalse(result.isGroupExists());
            assertNull(result.getStatus());
        }
    }

    @Test
    void getCurrentUserGroupStatus_WhenUserNotMember_ShouldReturnNoStatus() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(TEST_USER_ID, TEST_GROUP_ID))
                    .thenReturn(Optional.empty());

            // When
            GroupMemberStatusResponse result = groupService.getCurrentUserGroupStatus(TEST_GROUP_ID);

            // Then
            assertNotNull(result);
            assertTrue(result.isGroupExists());
            assertNull(result.getStatus());
        }
    }

    // ================= DELETE GROUP TESTS =================

    @Test
    void deleteGroup_WhenValidGroupId_ShouldDeleteGroupSuccessfully() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupMember));

            // When
            groupService.deleteGroup(TEST_GROUP_ID);

            // Then
            verify(groupRepository).save(any(Group.class));
        }
    }

    @Test
    void deleteGroup_WhenGroupIdIsNull_ShouldThrowGroupException() {
        // When & Then
        GroupException exception = assertThrows(
                GroupException.class,
                () -> groupService.deleteGroup(null)
        );

        assertEquals("Group ID cannot be null", exception.getMessage());
        verify(groupRepository, never()).deleteById(any());
    }

    @Test
    void deleteGroup_WhenGroupNotFound_ShouldThrowGroupException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(
                    GroupException.class,
                    () -> groupService.deleteGroup(TEST_GROUP_ID)
            );

            assertEquals("Group not found", exception.getMessage());
        }
    }

    // ================= GET USER PENDING GROUPS TESTS =================

    @Test
    void getUserPendingGroups_WhenValidUser_ShouldReturnPendingGroups() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            when(groupMemberRepository.findUserPendingGroups(TEST_USER_ID, GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(Arrays.asList(testGroupMember));

            // When
            List<UserPendingGroupResponse> result = groupService.getUserPendingGroups();

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Test
    void getUserPendingGroups_WhenNoPendingGroups_ShouldReturnEmptyList() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            when(groupMemberRepository.findUserPendingGroups(TEST_USER_ID, GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(Arrays.asList());

            // When
            List<UserPendingGroupResponse> result = groupService.getUserPendingGroups();

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    // ================= CANCEL JOIN GROUP REQUEST TESTS =================

    @Test
    void cancelJoinGroupRequest_WhenValidRequest_ShouldCancelRequestSuccessfully() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_USER_ID, TEST_GROUP_ID, GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(Optional.of(testGroupMember));
            when(groupMemberRepository.save(any(GroupMember.class))).thenReturn(testGroupMember);

            // When
            groupService.cancelJoinGroupRequest(cancelJoinGroupRequest);

            // Then
            verify(groupMemberRepository).save(any(GroupMember.class));
        }
    }

    @Test
    void cancelJoinGroupRequest_WhenRequestIsNull_ShouldThrowGroupException() {
        // When & Then
        GroupException exception = assertThrows(
                GroupException.class,
                () -> groupService.cancelJoinGroupRequest(null)
        );

        assertEquals("Cancel request cannot be null", exception.getMessage());
        verify(groupRepository, never()).findById(any());
    }

    @Test
    void cancelJoinGroupRequest_WhenGroupIdIsNull_ShouldThrowGroupException() {
        // Given
        cancelJoinGroupRequest.setGroupId(null);

        // When & Then
        GroupException exception = assertThrows(
                GroupException.class,
                () -> groupService.cancelJoinGroupRequest(cancelJoinGroupRequest)
        );

        assertEquals("Group ID cannot be null", exception.getMessage());
        verify(groupRepository, never()).findById(any());
    }

    @Test
    void cancelJoinGroupRequest_WhenGroupIdIsZero_ShouldThrowGroupException() {
        // Given
        cancelJoinGroupRequest.setGroupId(0);

        // When & Then
        GroupException exception = assertThrows(
                GroupException.class,
                () -> groupService.cancelJoinGroupRequest(cancelJoinGroupRequest)
        );

        assertEquals("Group ID must be a positive integer", exception.getMessage());
        verify(groupRepository, never()).findById(any());
    }

    @Test
    void cancelJoinGroupRequest_WhenGroupIdIsNegative_ShouldThrowGroupException() {
        // Given
        cancelJoinGroupRequest.setGroupId(-1);

        // When & Then
        GroupException exception = assertThrows(
                GroupException.class,
                () -> groupService.cancelJoinGroupRequest(cancelJoinGroupRequest)
        );

        assertEquals("Group ID must be a positive integer", exception.getMessage());
        verify(groupRepository, never()).findById(any());
    }

    @Test
    void cancelJoinGroupRequest_WhenGroupNotFound_ShouldThrowGroupException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(
                    GroupException.class,
                    () -> groupService.cancelJoinGroupRequest(cancelJoinGroupRequest)
            );

            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    void cancelJoinGroupRequest_WhenNoPendingRequest_ShouldThrowGroupException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_USER_ID, TEST_GROUP_ID, GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(
                    GroupException.class,
                    () -> groupService.cancelJoinGroupRequest(cancelJoinGroupRequest)
            );

            assertEquals("No pending join request found for this group", exception.getMessage());
        }
    }

    // ================= GET INVITED GROUP MEMBERS TESTS =================

    @Test
    void getInvitedGroupMembers_WhenValidGroupId_ShouldReturnInvitedMembers() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupMember));
            when(groupPermissionService.canViewInvitedMembers(GroupMemberRole.OWNER)).thenReturn(true);
            when(groupMemberRepository.findByGroupAndStatusAndUserIdNot(TEST_GROUP_ID, GroupMemberStatus.INVITED, TEST_USER_ID))
                    .thenReturn(Arrays.asList());

            // When
            List<InvitedGroupMemberResponse> result = groupService.getInvitedGroupMembers(TEST_GROUP_ID);

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    @Test
    void getInvitedGroupMembers_WhenGroupIdIsNull_ShouldThrowGroupException() {
        // When & Then
        GroupException exception = assertThrows(
                GroupException.class,
                () -> groupService.getInvitedGroupMembers(null)
        );

        assertEquals("Group ID cannot be null", exception.getMessage());
        verify(groupRepository, never()).findById(any());
    }

    @Test
    void getInvitedGroupMembers_WhenGroupIdIsZero_ShouldThrowGroupException() {
        // When & Then
        GroupException exception = assertThrows(
                GroupException.class,
                () -> groupService.getInvitedGroupMembers(0)
        );

        assertEquals("Group ID must be a positive integer", exception.getMessage());
        verify(groupRepository, never()).findById(any());
    }

    @Test
    void getInvitedGroupMembers_WhenGroupIdIsNegative_ShouldThrowGroupException() {
        // When & Then
        GroupException exception = assertThrows(
                GroupException.class,
                () -> groupService.getInvitedGroupMembers(-1)
        );

        assertEquals("Group ID must be a positive integer", exception.getMessage());
        verify(groupRepository, never()).findById(any());
    }

    @Test
    void getInvitedGroupMembers_WhenGroupNotFound_ShouldThrowGroupException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(
                    GroupException.class,
                    () -> groupService.getInvitedGroupMembers(TEST_GROUP_ID)
            );

            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    void getInvitedGroupMembers_WhenGroupInactive_ShouldThrowGroupException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            testGroup.setGroupIsActive(false);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(testGroup));

            // When & Then
            GroupException exception = assertThrows(
                    GroupException.class,
                    () -> groupService.getInvitedGroupMembers(TEST_GROUP_ID)
            );

            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    void getInvitedGroupMembers_WhenUserNotMember_ShouldThrowGroupException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(
                    GroupException.class,
                    () -> groupService.getInvitedGroupMembers(TEST_GROUP_ID)
            );

            assertEquals("You are not a member of this group", exception.getMessage());
        }
    }

    @Test
    void getInvitedGroupMembers_WhenUserLacksPermission_ShouldThrowGroupException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupMember));
            when(groupPermissionService.canViewInvitedMembers(GroupMemberRole.OWNER)).thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(
                    GroupException.class,
                    () -> groupService.getInvitedGroupMembers(TEST_GROUP_ID)
            );

            assertEquals("You don't have permission to view invited members", exception.getMessage());
        }
    }

    // ================= BOUNDARY TESTS =================

    @Test
    void getGroupDetail_WhenGroupIdIsOne_ShouldWorkCorrectly() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(groupRepository.findById(1)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(
                    GroupException.class,
                    () -> groupService.getGroupDetail(1)
            );

            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    void getGroupDetail_WhenGroupIdIsMaxValue_ShouldWorkCorrectly() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(groupRepository.findById(Integer.MAX_VALUE)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(
                    GroupException.class,
                    () -> groupService.getGroupDetail(Integer.MAX_VALUE)
            );

            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    void createGroupWithImage_WhenGroupNameIsVeryLong_ShouldWorkCorrectly() {
        // Given
        String veryLongName = "A".repeat(100); // Maximum allowed length
        createGroupRequest.setGroupName(veryLongName);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            
            // Mock the group that will be created by mapper
            Group mappedGroup = new Group();
            mappedGroup.setGroupName(veryLongName);
            mappedGroup.setGroupIsActive(true);
            
            // Mock the group that will be saved and returned
            Group savedGroup = new Group();
            savedGroup.setGroupId(TEST_GROUP_ID);
            savedGroup.setGroupName(veryLongName);
            savedGroup.setGroupIsActive(true);
            savedGroup.setGroupAvatarUrl("https://cdn.pixabay.com/photo/2016/11/14/17/39/group-1824145_1280.png");
            
            when(groupMapper.toEntity(createGroupRequest)).thenReturn(mappedGroup);
            when(groupRepository.save(any(Group.class))).thenReturn(savedGroup);
            when(groupMapper.toResponse(any(Group.class))).thenReturn(new GroupResponse());

            // When
            GroupResponse result = groupService.createGroupWithImage(createGroupRequest);

            // Then
            assertNotNull(result);
            verify(groupRepository).save(argThat(group -> 
                group.getGroupName().equals(veryLongName)
            ));
        }
    }

    @Test
    void createGroupWithImage_WhenGroupNameExceedsMaxLength_ShouldThrowGroupException() {
        // Given
        String tooLongName = "A".repeat(101); // Exceeds maximum length
        createGroupRequest.setGroupName(tooLongName);

        // When & Then
        GroupException exception = assertThrows(
                GroupException.class,
                () -> groupService.createGroupWithImage(createGroupRequest)
        );

        assertEquals("Group name cannot exceed 100 characters", exception.getMessage());
        verify(groupRepository, never()).save(any());
    }

    @Test
    void getInvitedGroupMembers_WhenGroupIdIsOne_ShouldWorkCorrectly() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(groupRepository.findById(1)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_USER_ID, 1, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupMember));
            when(groupPermissionService.canViewInvitedMembers(GroupMemberRole.OWNER)).thenReturn(true);
            when(groupMemberRepository.findByGroupAndStatusAndUserIdNot(1, GroupMemberStatus.INVITED, TEST_USER_ID))
                    .thenReturn(Arrays.asList());

            // When
            List<InvitedGroupMemberResponse> result = groupService.getInvitedGroupMembers(1);

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    @Test
    void getInvitedGroupMembers_WhenGroupIdIsMaxValue_ShouldWorkCorrectly() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testUser);
            when(groupRepository.findById(Integer.MAX_VALUE)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_USER_ID, Integer.MAX_VALUE, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupMember));
            when(groupPermissionService.canViewInvitedMembers(GroupMemberRole.OWNER)).thenReturn(true);
            when(groupMemberRepository.findByGroupAndStatusAndUserIdNot(Integer.MAX_VALUE, GroupMemberStatus.INVITED, TEST_USER_ID))
                    .thenReturn(Arrays.asList());

            // When
            List<InvitedGroupMemberResponse> result = groupService.getInvitedGroupMembers(Integer.MAX_VALUE);

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }
}
