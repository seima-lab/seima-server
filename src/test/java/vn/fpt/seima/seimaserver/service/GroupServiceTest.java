package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.fpt.seima.seimaserver.dto.response.group.InvitedGroupMemberResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.exception.GroupException;
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
@DisplayName("GroupService Tests")
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupPermissionService groupPermissionService;

    @InjectMocks
    private GroupServiceImpl groupService;

    private User mockCurrentUser;
    private Group mockGroup;
    private GroupMember mockCurrentUserMembership;
    private GroupMember mockInvitedMember1;
    private GroupMember mockInvitedMember2;
    private User mockInvitedUser1;
    private User mockInvitedUser2;

    // Test constants
    private static final Integer TEST_GROUP_ID = 1;
    private static final Integer TEST_CURRENT_USER_ID = 100;
    private static final Integer TEST_INVITED_USER_ID_1 = 200;
    private static final Integer TEST_INVITED_USER_ID_2 = 300;
    private static final String TEST_GROUP_NAME = "Test Group";
    private static final String TEST_CURRENT_USER_EMAIL = "current@example.com";
    private static final String TEST_INVITED_USER_EMAIL_1 = "invited1@example.com";
    private static final String TEST_INVITED_USER_EMAIL_2 = "invited2@example.com";

    @BeforeEach
    void setUp() {
        // Setup mock current user
        mockCurrentUser = User.builder()
                .userId(TEST_CURRENT_USER_ID)
                .userEmail(TEST_CURRENT_USER_EMAIL)
                .userFullName("Current User")
                .userIsActive(true)
                .build();

        // Setup mock group
        mockGroup = new Group();
        mockGroup.setGroupId(TEST_GROUP_ID);
        mockGroup.setGroupName(TEST_GROUP_NAME);
        mockGroup.setGroupIsActive(true);

        // Setup mock current user membership (ADMIN role)
        mockCurrentUserMembership = new GroupMember();
        mockCurrentUserMembership.setUser(mockCurrentUser);
        mockCurrentUserMembership.setGroup(mockGroup);
        mockCurrentUserMembership.setRole(GroupMemberRole.ADMIN);
        mockCurrentUserMembership.setStatus(GroupMemberStatus.ACTIVE);

        // Setup mock invited users
        mockInvitedUser1 = User.builder()
                .userId(TEST_INVITED_USER_ID_1)
                .userEmail(TEST_INVITED_USER_EMAIL_1)
                .userFullName("Invited User 1")
                .userAvatarUrl("https://example.com/avatar1.jpg")
                .userIsActive(true)
                .build();

        mockInvitedUser2 = User.builder()
                .userId(TEST_INVITED_USER_ID_2)
                .userEmail(TEST_INVITED_USER_EMAIL_2)
                .userFullName("Invited User 2")
                .userAvatarUrl("https://example.com/avatar2.jpg")
                .userIsActive(true)
                .build();

        // Setup mock invited members
        mockInvitedMember1 = new GroupMember();
        mockInvitedMember1.setUser(mockInvitedUser1);
        mockInvitedMember1.setGroup(mockGroup);
        mockInvitedMember1.setRole(GroupMemberRole.MEMBER);
        mockInvitedMember1.setStatus(GroupMemberStatus.INVITED);
        mockInvitedMember1.setJoinDate(LocalDateTime.now().minusDays(1));

        mockInvitedMember2 = new GroupMember();
        mockInvitedMember2.setUser(mockInvitedUser2);
        mockInvitedMember2.setGroup(mockGroup);
        mockInvitedMember2.setRole(GroupMemberRole.MEMBER);
        mockInvitedMember2.setStatus(GroupMemberStatus.INVITED);
        mockInvitedMember2.setJoinDate(LocalDateTime.now().minusHours(2));
    }

    @Test
    @DisplayName("N - Normal: Get invited members successfully")
    void getInvitedGroupMembers_Success() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_CURRENT_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(mockCurrentUserMembership));
            when(groupPermissionService.canViewInvitedMembers(GroupMemberRole.ADMIN)).thenReturn(true);
            when(groupMemberRepository.findByGroupAndStatusAndUserIdNot(TEST_GROUP_ID, GroupMemberStatus.INVITED, TEST_CURRENT_USER_ID))
                    .thenReturn(Arrays.asList(mockInvitedMember1, mockInvitedMember2));

            // When
            List<InvitedGroupMemberResponse> result = groupService.getInvitedGroupMembers(TEST_GROUP_ID);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            
            // Verify first invited member
            InvitedGroupMemberResponse firstMember = result.get(0);
            assertEquals(TEST_INVITED_USER_ID_1, firstMember.getUserId());
            assertEquals(TEST_INVITED_USER_EMAIL_1, firstMember.getUserEmail());
            assertEquals("Invited User 1", firstMember.getUserFullName());
            assertEquals("https://example.com/avatar1.jpg", firstMember.getUserAvatarUrl());
            assertEquals("MEMBER", firstMember.getAssignedRole());
            assertNotNull(firstMember.getInvitedAt());
            
            // Verify second invited member
            InvitedGroupMemberResponse secondMember = result.get(1);
            assertEquals(TEST_INVITED_USER_ID_2, secondMember.getUserId());
            assertEquals(TEST_INVITED_USER_EMAIL_2, secondMember.getUserEmail());
            assertEquals("Invited User 2", secondMember.getUserFullName());
            assertEquals("https://example.com/avatar2.jpg", secondMember.getUserAvatarUrl());
            assertEquals("MEMBER", secondMember.getAssignedRole());
            assertNotNull(secondMember.getInvitedAt());
        }
    }

    @Test
    @DisplayName("N - Normal: Get invited members when no invited members exist")
    void getInvitedGroupMembers_NoInvitedMembers() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_CURRENT_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(mockCurrentUserMembership));
            when(groupPermissionService.canViewInvitedMembers(GroupMemberRole.ADMIN)).thenReturn(true);
            when(groupMemberRepository.findByGroupAndStatusAndUserIdNot(TEST_GROUP_ID, GroupMemberStatus.INVITED, TEST_CURRENT_USER_ID))
                    .thenReturn(Arrays.asList());

            // When
            List<InvitedGroupMemberResponse> result = groupService.getInvitedGroupMembers(TEST_GROUP_ID);

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    @Test
    @DisplayName("A - Abnormal: Group ID is null")
    void getInvitedGroupMembers_NullGroupId() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
                () -> groupService.getInvitedGroupMembers(null));
        
        assertEquals("Group ID cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("A - Abnormal: Group ID is zero")
    void getInvitedGroupMembers_ZeroGroupId() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
                () -> groupService.getInvitedGroupMembers(0));
        
        assertEquals("Group ID must be a positive integer", exception.getMessage());
    }

    @Test
    @DisplayName("A - Abnormal: Group ID is negative")
    void getInvitedGroupMembers_NegativeGroupId() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
                () -> groupService.getInvitedGroupMembers(-1));
        
        assertEquals("Group ID must be a positive integer", exception.getMessage());
    }

    @Test
    @DisplayName("A - Abnormal: Group not found")
    void getInvitedGroupMembers_GroupNotFound() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupService.getInvitedGroupMembers(TEST_GROUP_ID));
            
            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    @DisplayName("A - Abnormal: Group is inactive")
    void getInvitedGroupMembers_GroupInactive() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            
            mockGroup.setGroupIsActive(false);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupService.getInvitedGroupMembers(TEST_GROUP_ID));
            
            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    @DisplayName("A - Abnormal: Current user is not a member of the group")
    void getInvitedGroupMembers_UserNotMember() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_CURRENT_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupService.getInvitedGroupMembers(TEST_GROUP_ID));
            
            assertEquals("You are not a member of this group", exception.getMessage());
        }
    }

    @Test
    @DisplayName("A - Abnormal: User lacks permission to view invited members")
    void getInvitedGroupMembers_UserLacksPermission() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            
            mockCurrentUserMembership.setRole(GroupMemberRole.MEMBER);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_CURRENT_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(mockCurrentUserMembership));
            when(groupPermissionService.canViewInvitedMembers(GroupMemberRole.MEMBER)).thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupService.getInvitedGroupMembers(TEST_GROUP_ID));
            
            assertEquals("You don't have permission to view invited members", exception.getMessage());
        }
    }

    @Test
    @DisplayName("B - Boundary: Group ID is 1 (minimum valid value)")
    void getInvitedGroupMembers_MinimumGroupId() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            
            when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_CURRENT_USER_ID, 1, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(mockCurrentUserMembership));
            when(groupPermissionService.canViewInvitedMembers(GroupMemberRole.ADMIN)).thenReturn(true);
            when(groupMemberRepository.findByGroupAndStatusAndUserIdNot(1, GroupMemberStatus.INVITED, TEST_CURRENT_USER_ID))
                    .thenReturn(Arrays.asList());

            // When
            List<InvitedGroupMemberResponse> result = groupService.getInvitedGroupMembers(1);

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    @Test
    @DisplayName("B - Boundary: Group ID is Integer.MAX_VALUE")
    void getInvitedGroupMembers_MaximumGroupId() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            
            when(groupRepository.findById(Integer.MAX_VALUE)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_CURRENT_USER_ID, Integer.MAX_VALUE, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(mockCurrentUserMembership));
            when(groupPermissionService.canViewInvitedMembers(GroupMemberRole.ADMIN)).thenReturn(true);
            when(groupMemberRepository.findByGroupAndStatusAndUserIdNot(Integer.MAX_VALUE, GroupMemberStatus.INVITED, TEST_CURRENT_USER_ID))
                    .thenReturn(Arrays.asList());

            // When
            List<InvitedGroupMemberResponse> result = groupService.getInvitedGroupMembers(Integer.MAX_VALUE);

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    @Test
    @DisplayName("N - Normal: Owner can view invited members")
    void getInvitedGroupMembers_OwnerPermission() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            
            mockCurrentUserMembership.setRole(GroupMemberRole.OWNER);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_CURRENT_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(mockCurrentUserMembership));
            when(groupPermissionService.canViewInvitedMembers(GroupMemberRole.OWNER)).thenReturn(true);
            when(groupMemberRepository.findByGroupAndStatusAndUserIdNot(TEST_GROUP_ID, GroupMemberStatus.INVITED, TEST_CURRENT_USER_ID))
                    .thenReturn(Arrays.asList(mockInvitedMember1));

            // When
            List<InvitedGroupMemberResponse> result = groupService.getInvitedGroupMembers(TEST_GROUP_ID);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(TEST_INVITED_USER_ID_1, result.get(0).getUserId());
        }
    }

    @Test
    @DisplayName("N - Normal: Admin can view invited members")
    void getInvitedGroupMembers_AdminPermission() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            
            mockCurrentUserMembership.setRole(GroupMemberRole.ADMIN);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(TEST_CURRENT_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(mockCurrentUserMembership));
            when(groupPermissionService.canViewInvitedMembers(GroupMemberRole.ADMIN)).thenReturn(true);
            when(groupMemberRepository.findByGroupAndStatusAndUserIdNot(TEST_GROUP_ID, GroupMemberStatus.INVITED, TEST_CURRENT_USER_ID))
                    .thenReturn(Arrays.asList(mockInvitedMember1));

            // When
            List<InvitedGroupMemberResponse> result = groupService.getInvitedGroupMembers(TEST_GROUP_ID);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(TEST_INVITED_USER_ID_1, result.get(0).getUserId());
        }
    }
} 