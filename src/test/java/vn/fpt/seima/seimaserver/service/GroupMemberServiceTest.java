package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.fpt.seima.seimaserver.dto.request.group.AcceptGroupMemberRequest;
import vn.fpt.seima.seimaserver.dto.request.group.RejectGroupMemberRequest;
import vn.fpt.seima.seimaserver.dto.request.group.UpdateMemberRoleRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberListResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberResponse;
import vn.fpt.seima.seimaserver.dto.response.group.PendingGroupMemberListResponse;
import vn.fpt.seima.seimaserver.dto.response.group.PendingGroupMemberResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.exception.GroupException;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.repository.GroupRepository;
import vn.fpt.seima.seimaserver.service.impl.GroupMemberServiceImpl;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class GroupMemberServiceTest {

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupPermissionService groupPermissionService;

    @InjectMocks
    private GroupMemberServiceImpl groupMemberService;

    private Group testGroup;
    private User testCurrentUser;
    private User testLeader;
    private User testMember1;
    private User testMember2;
    private GroupMember testGroupLeader;
    private GroupMember testGroupMember1;
    private GroupMember testGroupMember2;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        testGroup = new Group();
        testGroup.setGroupId(1);
        testGroup.setGroupName("Test Group");
        testGroup.setGroupAvatarUrl("https://example.com/group-avatar.jpg");
        testGroup.setGroupCreatedDate(LocalDateTime.now());
        testGroup.setGroupIsActive(true);

        testCurrentUser = User.builder()
                .userId(1)
                .userFullName("Current User")
                .userAvatarUrl("https://example.com/current-avatar.jpg")
                .userEmail("current@example.com")
                .userIsActive(true)
                .build();

        testLeader = User.builder()
                .userId(2)
                .userFullName("Group Leader")
                .userAvatarUrl("https://example.com/leader-avatar.jpg")
                .userEmail("leader@example.com")
                .userIsActive(true)
                .build();

        testMember1 = User.builder()
                .userId(3)
                .userFullName("Member One")
                .userAvatarUrl("https://example.com/member1-avatar.jpg")
                .userEmail("member1@example.com")
                .userIsActive(true)
                .build();

        testMember2 = User.builder()
                .userId(4)
                .userFullName("Member Two")
                .userAvatarUrl("https://example.com/member2-avatar.jpg")
                .userEmail("member2@example.com")
                .userIsActive(true)
                .build();

        testGroupLeader = createGroupMember(1, testGroup, testLeader, GroupMemberRole.OWNER, GroupMemberStatus.ACTIVE);
        testGroupMember1 = createGroupMember(2, testGroup, testCurrentUser, GroupMemberRole.MEMBER, GroupMemberStatus.ACTIVE);
        testGroupMember2 = createGroupMember(3, testGroup, testMember2, GroupMemberRole.MEMBER, GroupMemberStatus.ACTIVE);
    }

    private GroupMember createGroupMember(Integer id, Group group, User user, GroupMemberRole role, GroupMemberStatus status) {
        GroupMember groupMember = new GroupMember();
        groupMember.setGroupMemberId(id);
        groupMember.setGroup(group);
        groupMember.setUser(user);
        groupMember.setRole(role);
        groupMember.setStatus(status);
        groupMember.setJoinDate(LocalDateTime.now().minusDays(id * 10));
        return groupMember;
    }

    private Group createGroup(Integer id, String name, boolean isActive) {
        Group group = new Group();
        group.setGroupId(id);
        group.setGroupName(name);
        group.setGroupAvatarUrl("https://example.com/group-avatar.jpg");
        group.setGroupCreatedDate(LocalDateTime.now());
        group.setGroupIsActive(isActive);
        return group;
    }

    private User createUser(Integer id, String name, boolean isActive) {
        return User.builder()
                .userId(id)
                .userFullName(name)
                .userAvatarUrl("https://example.com/avatar.jpg")
                .userEmail(name.toLowerCase().replace(" ", "") + "@example.com")
                .userIsActive(isActive)
                .build();
    }

    // ===== GET ACTIVE GROUP MEMBERS TESTS =====

    @Test
    void getActiveGroupMembers_Success() {
        // Given
        Integer groupId = 1;
        List<GroupMember> allMembers = Arrays.asList(testGroupLeader, testGroupMember1, testGroupMember2);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testCurrentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    testCurrentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(true);
            when(groupMemberRepository.findGroupOwner(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupLeader));
            when(groupMemberRepository.findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(allMembers);

            // When
            GroupMemberListResponse result = groupMemberService.getActiveGroupMembers(groupId);

            // Then
            assertNotNull(result);
            assertEquals(testGroup.getGroupId(), result.getGroupId());
            assertEquals(testGroup.getGroupName(), result.getGroupName());
            assertEquals(testGroup.getGroupAvatarUrl(), result.getGroupAvatarUrl());
            assertEquals(3, result.getTotalMembersCount());
            assertEquals(GroupMemberRole.MEMBER, result.getCurrentUserRole());

            // Verify leader
            GroupMemberResponse leader = result.getGroupLeader();
            assertNotNull(leader);
            assertEquals(testLeader.getUserId(), leader.getUserId());
            assertEquals(testLeader.getUserFullName(), leader.getUserFullName());
            assertEquals(GroupMemberRole.OWNER, leader.getRole());

            // Verify members (should not include leader)
            List<GroupMemberResponse> members = result.getMembers();
            assertNotNull(members);
            assertEquals(2, members.size());
            assertFalse(members.stream().anyMatch(member -> 
                member.getUserId().equals(testLeader.getUserId())));
        }
    }

    @Test
    void getActiveGroupMembers_ThrowsException_WhenGroupIdIsNull() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupMemberService.getActiveGroupMembers(null));

        assertEquals("Group ID cannot be null", exception.getMessage());
        verifyNoInteractions(groupRepository, groupMemberRepository);
    }

    @Test
    void getActiveGroupMembers_ThrowsException_WhenGroupNotFound() {
        // Given
        Integer groupId = 999;

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testCurrentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupMemberService.getActiveGroupMembers(groupId));

            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    void getActiveGroupMembers_ThrowsException_WhenGroupIsInactive() {
        // Given
        Integer groupId = 1;
        testGroup.setGroupIsActive(false);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testCurrentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupMemberService.getActiveGroupMembers(groupId));

            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    void getActiveGroupMembers_ThrowsException_WhenUserNotActiveMember() {
        // Given
        Integer groupId = 1;

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testCurrentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    testCurrentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupMemberService.getActiveGroupMembers(groupId));

            assertEquals("You don't have permission to view this group's members", exception.getMessage());
        }
    }

    @Test
    void getActiveGroupMembers_ThrowsException_WhenGroupLeaderNotFound() {
        // Given
        Integer groupId = 1;

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testCurrentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    testCurrentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(true);
            when(groupMemberRepository.findGroupOwner(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupMemberService.getActiveGroupMembers(groupId));

            assertEquals("Group owner not found for group ID: " + groupId, exception.getMessage());
        }
    }

    @Test
    void getActiveGroupMembers_ShouldReturnCorrectUserRole_WhenUserIsAdmin() {
        // Given
        Integer groupId = 1;
        testGroupMember1.setRole(GroupMemberRole.ADMIN);
        List<GroupMember> allMembers = Arrays.asList(testGroupLeader, testGroupMember1, testGroupMember2);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testCurrentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    testCurrentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(true);
            when(groupMemberRepository.findGroupOwner(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupLeader));
            when(groupMemberRepository.findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(allMembers);

            // When
            GroupMemberListResponse result = groupMemberService.getActiveGroupMembers(groupId);

            // Then
            assertEquals(GroupMemberRole.ADMIN, result.getCurrentUserRole());
        }
    }

    @Test
    void getActiveGroupMembers_ShouldReturnOnlyLeader_WhenOnlyLeaderExists() {
        // Given
        Integer groupId = 1;
        List<GroupMember> onlyLeader = Arrays.asList(testGroupLeader);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testLeader); // Current user is the leader
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    testLeader.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(true);
            when(groupMemberRepository.findGroupOwner(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupLeader));
            when(groupMemberRepository.findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(onlyLeader);

            // When
            GroupMemberListResponse result = groupMemberService.getActiveGroupMembers(groupId);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalMembersCount());
            assertEquals(GroupMemberRole.OWNER, result.getCurrentUserRole());
            assertEquals(0, result.getMembers().size()); // No members other than leader
        }
    }

    @Test
    void getActiveGroupMembers_ShouldFilterOutInactiveUsers() {
        // Given
        Integer groupId = 1;
        testMember2.setUserIsActive(false); // Member2 is inactive

        List<GroupMember> allMembers = Arrays.asList(testGroupLeader, testGroupMember1, testGroupMember2);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testCurrentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    testCurrentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(true);
            when(groupMemberRepository.findGroupOwner(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupLeader));
            when(groupMemberRepository.findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(allMembers);

            // When
            GroupMemberListResponse result = groupMemberService.getActiveGroupMembers(groupId);

            // Then
            assertNotNull(result);
            assertEquals(2, result.getTotalMembersCount()); // Total count only includes active users
            assertEquals(1, result.getMembers().size()); // But filtered members only include active ones
            assertFalse(result.getMembers().stream().anyMatch(member ->
                    member.getUserId().equals(testMember2.getUserId())));
        }
    }

    // ===============================
    // Tests for handleUserAccountDeactivation
    // ===============================
    
    // NOTE: Integration test for the new API endpoint /api/v1/users/deactivate
    // should be created to test the full flow:
    // 1. User calls deactivate API
    // 2. handleUserAccountDeactivation is called
    // 3. User account is deactivated
    // 4. Group leadership transfer occurs if applicable

    @Test
    void handleUserAccountDeactivation_ShouldPromoteMemberToAdmin_WhenLeaderDeactivatesAndMembersExist() {
        // Given
        Integer userId = 1;
        Group group = createGroup(1, "Test Group", true);
        User leader = createUser(1, "Leader", true);
        User member = createUser(2, "Member", true);
        
        GroupMember leaderMembership = createGroupMember(1, group, leader, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE);
        GroupMember memberMembership = createGroupMember(2, group, member, GroupMemberRole.MEMBER, GroupMemberStatus.ACTIVE);
        
        when(groupMemberRepository.findByUserIdAndRole(userId, GroupMemberRole.ADMIN))
                .thenReturn(List.of(leaderMembership));
        when(groupMemberRepository.findActiveGroupMembers(1, GroupMemberStatus.ACTIVE))
                .thenReturn(List.of(leaderMembership, memberMembership));

        // When
        groupMemberService.handleUserAccountDeactivation(userId);

        // Then
        verify(groupMemberRepository).save(memberMembership);
        assertThat(memberMembership.getRole()).isEqualTo(GroupMemberRole.ADMIN);
    }

    @Test
    void handleUserAccountDeactivation_ShouldDeactivateGroup_WhenNoActiveMembersLeft() {
        // Given
        Integer userId = 1;
        Group group = createGroup(1, "Test Group", true);
        User leader = createUser(1, "Leader", true);
        
        GroupMember leaderMembership = createGroupMember(1, group, leader, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE);
        
        when(groupMemberRepository.findByUserIdAndRole(userId, GroupMemberRole.ADMIN))
                .thenReturn(List.of(leaderMembership));
        when(groupMemberRepository.findActiveGroupMembers(1, GroupMemberStatus.ACTIVE))
                .thenReturn(List.of(leaderMembership));

        // When
        groupMemberService.handleUserAccountDeactivation(userId);

        // Then
        verify(groupRepository).save(group);
        assertThat(group.getGroupIsActive()).isFalse();
    }

    @Test
    void handleUserAccountDeactivation_ShouldSkipInactiveGroups() {
        // Given
        Integer userId = 1;
        Group inactiveGroup = createGroup(1, "Inactive Group", false);
        User leader = createUser(1, "Leader", true);
        
        GroupMember leaderMembership = createGroupMember(1, inactiveGroup, leader, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE);
        
        when(groupMemberRepository.findByUserIdAndRole(userId, GroupMemberRole.ADMIN))
                .thenReturn(List.of(leaderMembership));

        // When
        groupMemberService.handleUserAccountDeactivation(userId);

        // Then
        verify(groupRepository, never()).save(any());
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void handleUserAccountDeactivation_ShouldDoNothing_WhenOtherActiveAdminsExist() {
        // Given
        Integer userId = 1;
        Group group = createGroup(1, "Test Group", true);
        User leader = createUser(1, "Leader", true);
        User admin = createUser(2, "Admin", true);
        
        GroupMember leaderMembership = createGroupMember(1, group, leader, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE);
        GroupMember adminMembership = createGroupMember(2, group, admin, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE);
        
        when(groupMemberRepository.findByUserIdAndRole(userId, GroupMemberRole.ADMIN))
                .thenReturn(List.of(leaderMembership));
        when(groupMemberRepository.findActiveGroupMembers(1, GroupMemberStatus.ACTIVE))
                .thenReturn(List.of(leaderMembership, adminMembership));

        // When
        groupMemberService.handleUserAccountDeactivation(userId);

        // Then
        verify(groupRepository, never()).save(any());
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void handleUserAccountDeactivation_ShouldThrowException_WhenUserIdIsNull() {
        // When & Then
        assertThatThrownBy(() -> groupMemberService.handleUserAccountDeactivation(null))
                .isInstanceOf(GroupException.class)
                .hasMessage("User ID cannot be null");
    }

    @Test
    void handleUserAccountDeactivation_ShouldFilterOutInactiveUsers() {
        // Given
        Integer userId = 1;
        Group group = createGroup(1, "Test Group", true);
        User leader = createUser(1, "Leader", true);
        User activeMember = createUser(2, "Active Member", true);
        User inactiveMember = createUser(3, "Inactive Member", false);
        
        GroupMember leaderMembership = createGroupMember(1, group, leader, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE);
        GroupMember activeMemberMembership = createGroupMember(2, group, activeMember, GroupMemberRole.MEMBER, GroupMemberStatus.ACTIVE);
        GroupMember inactiveMemberMembership = createGroupMember(3, group, inactiveMember, GroupMemberRole.MEMBER, GroupMemberStatus.ACTIVE);
        
        when(groupMemberRepository.findByUserIdAndRole(userId, GroupMemberRole.ADMIN))
                .thenReturn(List.of(leaderMembership));
        when(groupMemberRepository.findActiveGroupMembers(1, GroupMemberStatus.ACTIVE))
                .thenReturn(List.of(leaderMembership, activeMemberMembership, inactiveMemberMembership));

        // When
        groupMemberService.handleUserAccountDeactivation(userId);

        // Then - should promote active member, not inactive one
        verify(groupMemberRepository).save(activeMemberMembership);
        assertThat(activeMemberMembership.getRole()).isEqualTo(GroupMemberRole.ADMIN);
        assertThat(inactiveMemberMembership.getRole()).isEqualTo(GroupMemberRole.MEMBER); // unchanged
    }

    @Test
    @DisplayName("removeMemberFromGroup - Should successfully remove member when owner removes regular member")
    void removeMemberFromGroup_ShouldSuccessfullyRemoveMember_WhenAdminRemovesRegularMember() {
        // Given
        Integer groupId = 1;
        Integer memberUserId = 2;
        Integer ownerUserId = 3;
        User ownerUser = createTestUser(ownerUserId, "owner@example.com", "Owner User");
        User memberUser = createTestUser(memberUserId, "member@example.com", "Member User");
        Group group = createTestGroup(groupId, "Test Group");

        // Create owner member
        GroupMember ownerMember = new GroupMember();
        ownerMember.setUser(ownerUser);
        ownerMember.setGroup(group);
        ownerMember.setRole(GroupMemberRole.OWNER);
        ownerMember.setStatus(GroupMemberStatus.ACTIVE);

        // Create member to be removed
        GroupMember memberToRemove = new GroupMember();
        memberToRemove.setUser(memberUser);
        memberToRemove.setGroup(group);
        memberToRemove.setRole(GroupMemberRole.MEMBER);
        memberToRemove.setStatus(GroupMemberStatus.ACTIVE);

        // Mock current user
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(ownerUser);

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(ownerUserId, groupId))
                    .thenReturn(Optional.of(ownerMember));
            when(groupMemberRepository.findByUserIdAndGroupId(memberUserId, groupId))
                    .thenReturn(Optional.of(memberToRemove));
            
            // Mock GroupPermissionService
            when(groupPermissionService.canRemoveMember(GroupMemberRole.OWNER, GroupMemberRole.MEMBER))
                    .thenReturn(true);

            // When
            groupMemberService.removeMemberFromGroup(groupId, memberUserId);

            // Then
            verify(groupMemberRepository).save(memberToRemove);
            assertEquals(GroupMemberStatus.LEFT, memberToRemove.getStatus());
        }
    }

    @Test
    @DisplayName("removeMemberFromGroup - Should throw exception when group ID is null")
    void removeMemberFromGroup_ShouldThrowException_WhenGroupIdIsNull() {
        // Given
        Integer groupId = null;
        Integer memberUserId = 2;

        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
                () -> groupMemberService.removeMemberFromGroup(groupId, memberUserId));
        assertEquals("Group ID cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("removeMemberFromGroup - Should throw exception when member user ID is null")
    void removeMemberFromGroup_ShouldThrowException_WhenMemberUserIdIsNull() {
        // Given
        Integer groupId = 1;
        Integer memberUserId = null;

        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
                () -> groupMemberService.removeMemberFromGroup(groupId, memberUserId));
        assertEquals("Member user ID cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("removeMemberFromGroup - Should throw exception when group not found")
    void removeMemberFromGroup_ShouldThrowException_WhenGroupNotFound() {
        // Given
        Integer groupId = 1;
        Integer memberUserId = 2;
        User adminUser = createTestUser(3, "admin@example.com", "Admin User");

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(adminUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupMemberService.removeMemberFromGroup(groupId, memberUserId));
            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    @DisplayName("removeMemberFromGroup - Should throw exception when group is inactive")
    void removeMemberFromGroup_ShouldThrowException_WhenGroupIsInactive() {
        // Given
        Integer groupId = 1;
        Integer memberUserId = 2;
        User adminUser = createTestUser(3, "admin@example.com", "Admin User");
        Group inactiveGroup = createTestGroup(groupId, "Inactive Group");
        inactiveGroup.setGroupIsActive(false);

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(adminUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(inactiveGroup));

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupMemberService.removeMemberFromGroup(groupId, memberUserId));
            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    @DisplayName("removeMemberFromGroup - Should throw exception when current user is not admin")
    void removeMemberFromGroup_ShouldThrowException_WhenCurrentUserIsNotAdmin() {
        // Given
        Integer groupId = 1;
        Integer memberUserId = 3;

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testCurrentUser);
            
            GroupMember currentUserMember = createGroupMember(1, testGroup, testCurrentUser, 
                    GroupMemberRole.MEMBER, GroupMemberStatus.ACTIVE);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(testCurrentUser.getUserId(), groupId))
                    .thenReturn(Optional.of(currentUserMember));

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupMemberService.removeMemberFromGroup(groupId, memberUserId));

            assertEquals("Only group administrators and owners can remove members", exception.getMessage());
        }
    }

    @Test
    @DisplayName("removeMemberFromGroup - Should throw exception when member not found in group")
    void removeMemberFromGroup_ShouldThrowException_WhenMemberNotFoundInGroup() {
        // Given
        Integer groupId = 1;
        Integer memberUserId = 2;
        Integer ownerUserId = 3;
        User ownerUser = createTestUser(ownerUserId, "owner@example.com", "Owner User");
        Group group = createTestGroup(groupId, "Test Group");

        GroupMember ownerMember = new GroupMember();
        ownerMember.setUser(ownerUser);
        ownerMember.setGroup(group);
        ownerMember.setRole(GroupMemberRole.OWNER);
        ownerMember.setStatus(GroupMemberStatus.ACTIVE);

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(ownerUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(ownerUserId, groupId))
                    .thenReturn(Optional.of(ownerMember));
            when(groupMemberRepository.findByUserIdAndGroupId(memberUserId, groupId))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupMemberService.removeMemberFromGroup(groupId, memberUserId));
            assertEquals("Member not found in this group", exception.getMessage());
        }
    }

    @Test
    @DisplayName("removeMemberFromGroup - Should throw exception when member is not active")
    void removeMemberFromGroup_ShouldThrowException_WhenMemberIsNotActive() {
        // Given
        Integer groupId = 1;
        Integer memberUserId = 2;
        Integer ownerUserId = 3;
        User ownerUser = createTestUser(ownerUserId, "owner@example.com", "Owner User");
        User memberUser = createTestUser(memberUserId, "member@example.com", "Member User");
        Group group = createTestGroup(groupId, "Test Group");

        GroupMember ownerMember = new GroupMember();
        ownerMember.setUser(ownerUser);
        ownerMember.setGroup(group);
        ownerMember.setRole(GroupMemberRole.OWNER);
        ownerMember.setStatus(GroupMemberStatus.ACTIVE);

        GroupMember inactiveMember = new GroupMember();
        inactiveMember.setUser(memberUser);
        inactiveMember.setGroup(group);
        inactiveMember.setRole(GroupMemberRole.MEMBER);
        inactiveMember.setStatus(GroupMemberStatus.LEFT); // Not active

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(ownerUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(ownerUserId, groupId))
                    .thenReturn(Optional.of(ownerMember));
            when(groupMemberRepository.findByUserIdAndGroupId(memberUserId, groupId))
                    .thenReturn(Optional.of(inactiveMember));

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupMemberService.removeMemberFromGroup(groupId, memberUserId));
            assertEquals("Member is not currently active in this group", exception.getMessage());
        }
    }

    @Test
    @DisplayName("removeMemberFromGroup - Should throw exception when member user account is inactive")
    void removeMemberFromGroup_ShouldThrowException_WhenMemberUserAccountIsInactive() {
        // Given
        Integer groupId = 1;
        Integer memberUserId = 2;
        Integer ownerUserId = 3;
        User ownerUser = createTestUser(ownerUserId, "owner@example.com", "Owner User");
        User memberUser = createTestUser(memberUserId, "member@example.com", "Member User");
        memberUser.setUserIsActive(false); // Inactive user account
        Group group = createTestGroup(groupId, "Test Group");

        GroupMember ownerMember = new GroupMember();
        ownerMember.setUser(ownerUser);
        ownerMember.setGroup(group);
        ownerMember.setRole(GroupMemberRole.OWNER);
        ownerMember.setStatus(GroupMemberStatus.ACTIVE);

        GroupMember activeMember = new GroupMember();
        activeMember.setUser(memberUser);
        activeMember.setGroup(group);
        activeMember.setRole(GroupMemberRole.MEMBER);
        activeMember.setStatus(GroupMemberStatus.ACTIVE);

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(ownerUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(ownerUserId, groupId))
                    .thenReturn(Optional.of(ownerMember));
            when(groupMemberRepository.findByUserIdAndGroupId(memberUserId, groupId))
                    .thenReturn(Optional.of(activeMember));

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupMemberService.removeMemberFromGroup(groupId, memberUserId));
            assertEquals("Cannot remove inactive user account", exception.getMessage());
        }
    }

    @Test
    @DisplayName("removeMemberFromGroup - Should throw exception when trying to remove the owner")
    void removeMemberFromGroup_ShouldThrowException_WhenTryingToRemoveLastAdmin() {
        // Given
        Integer groupId = 1;
        Integer ownerUserId = 2;
        Integer adminUserId = 3;
        User ownerUser = createTestUser(ownerUserId, "owner@example.com", "Owner User");
        User adminUser = createTestUser(adminUserId, "admin@example.com", "Admin User");
        Group group = createTestGroup(groupId, "Test Group");

        GroupMember ownerMember = new GroupMember();
        ownerMember.setUser(ownerUser);
        ownerMember.setGroup(group);
        ownerMember.setRole(GroupMemberRole.OWNER);
        ownerMember.setStatus(GroupMemberStatus.ACTIVE);

        GroupMember adminMember = new GroupMember();
        adminMember.setUser(adminUser);
        adminMember.setGroup(group);
        adminMember.setRole(GroupMemberRole.ADMIN);
        adminMember.setStatus(GroupMemberStatus.ACTIVE);

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(adminUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(adminUserId, groupId))
                    .thenReturn(Optional.of(adminMember));
            when(groupMemberRepository.findByUserIdAndGroupId(ownerUserId, groupId))
                    .thenReturn(Optional.of(ownerMember));

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupMemberService.removeMemberFromGroup(groupId, ownerUserId));
            assertEquals("Group owner cannot be removed from the group", exception.getMessage());
        }
    }

    @Test
    @DisplayName("removeMemberFromGroup - Should throw exception when admin tries to remove another admin")
    void removeMemberFromGroup_ShouldThrowException_WhenAdminTriesToRemoveLastAdmin() {
        // Given
        Integer groupId = 1;
        Integer admin1UserId = 2;
        Integer admin2UserId = 3;
        User admin1User = createTestUser(admin1UserId, "admin1@example.com", "Admin 1 User");
        User admin2User = createTestUser(admin2UserId, "admin2@example.com", "Admin 2 User");
        Group group = createTestGroup(groupId, "Test Group");

        GroupMember admin1Member = new GroupMember();
        admin1Member.setUser(admin1User);
        admin1Member.setGroup(group);
        admin1Member.setRole(GroupMemberRole.ADMIN);
        admin1Member.setStatus(GroupMemberStatus.ACTIVE);

        GroupMember admin2Member = new GroupMember();
        admin2Member.setUser(admin2User);
        admin2Member.setGroup(group);
        admin2Member.setRole(GroupMemberRole.ADMIN);
        admin2Member.setStatus(GroupMemberStatus.ACTIVE);

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(admin1User);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(admin1UserId, groupId))
                    .thenReturn(Optional.of(admin1Member));
            when(groupMemberRepository.findByUserIdAndGroupId(admin2UserId, groupId))
                    .thenReturn(Optional.of(admin2Member));
            
            // Mock GroupPermissionService - admin cannot remove another admin
            when(groupPermissionService.canRemoveMember(GroupMemberRole.ADMIN, GroupMemberRole.ADMIN))
                    .thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupMemberService.removeMemberFromGroup(groupId, admin2UserId));
            assertEquals("Insufficient permission to remove this member", exception.getMessage());
        }
    }

    @Test
    @DisplayName("removeMemberFromGroup - Should successfully remove admin when owner removes admin")
    void removeMemberFromGroup_ShouldSuccessfullyRemoveAdmin_WhenThereAreMultipleAdmins() {
        // Given
        Integer groupId = 1;
        Integer ownerUserId = 2;
        Integer adminUserId = 3;
        Integer anotherAdminUserId = 4;
        User ownerUser = createTestUser(ownerUserId, "owner@example.com", "Owner User");
        User adminUser = createTestUser(adminUserId, "admin@example.com", "Admin User");
        User anotherAdminUser = createTestUser(anotherAdminUserId, "admin2@example.com", "Admin 2 User");
        Group group = createTestGroup(groupId, "Test Group");

        GroupMember ownerMember = new GroupMember();
        ownerMember.setUser(ownerUser);
        ownerMember.setGroup(group);
        ownerMember.setRole(GroupMemberRole.OWNER);
        ownerMember.setStatus(GroupMemberStatus.ACTIVE);

        GroupMember adminMember = new GroupMember();
        adminMember.setUser(adminUser);
        adminMember.setGroup(group);
        adminMember.setRole(GroupMemberRole.ADMIN);
        adminMember.setStatus(GroupMemberStatus.ACTIVE);

        GroupMember anotherAdminMember = new GroupMember();
        anotherAdminMember.setUser(anotherAdminUser);
        anotherAdminMember.setGroup(group);
        anotherAdminMember.setRole(GroupMemberRole.ADMIN);
        anotherAdminMember.setStatus(GroupMemberStatus.ACTIVE);

        // All active members including owner and admins
        List<GroupMember> allActiveMembers = Arrays.asList(ownerMember, adminMember, anotherAdminMember);

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(ownerUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(ownerUserId, groupId))
                    .thenReturn(Optional.of(ownerMember));
            when(groupMemberRepository.findByUserIdAndGroupId(adminUserId, groupId))
                    .thenReturn(Optional.of(adminMember));
            // Mock all active group members for the validation logic
            when(groupMemberRepository.findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(allActiveMembers);
            
            // Mock GroupPermissionService
            when(groupPermissionService.canRemoveMember(GroupMemberRole.OWNER, GroupMemberRole.ADMIN))
                    .thenReturn(true);
            when(groupPermissionService.canRemoveLastAdmin(GroupMemberRole.OWNER, true, 2))
                    .thenReturn(true);

            // When
            groupMemberService.removeMemberFromGroup(groupId, adminUserId);

            // Then
            verify(groupMemberRepository).save(adminMember);
            assertEquals(GroupMemberStatus.LEFT, adminMember.getStatus());
        }
    }

    @Test
    @DisplayName("removeMemberFromGroup - Admin cannot remove themselves")
    void removeMemberFromGroup_ShouldSuccessfullyRemoveSelf_WhenThereAreMultipleAdmins() {
        // Given
        Integer groupId = 1;
        Integer ownerUserId = 2;
        Integer adminUserId = 3;
        Integer anotherAdminUserId = 4;
        User ownerUser = createTestUser(ownerUserId, "owner@example.com", "Owner User");
        User adminUser = createTestUser(adminUserId, "admin@example.com", "Admin User");
        User anotherAdminUser = createTestUser(anotherAdminUserId, "admin2@example.com", "Admin 2 User");
        Group group = createTestGroup(groupId, "Test Group");

        GroupMember ownerMember = new GroupMember();
        ownerMember.setUser(ownerUser);
        ownerMember.setGroup(group);
        ownerMember.setRole(GroupMemberRole.OWNER);
        ownerMember.setStatus(GroupMemberStatus.ACTIVE);

        GroupMember adminMember = new GroupMember();
        adminMember.setUser(adminUser);
        adminMember.setGroup(group);
        adminMember.setRole(GroupMemberRole.ADMIN);
        adminMember.setStatus(GroupMemberStatus.ACTIVE);

        GroupMember anotherAdminMember = new GroupMember();
        anotherAdminMember.setUser(anotherAdminUser);
        anotherAdminMember.setGroup(group);
        anotherAdminMember.setRole(GroupMemberRole.ADMIN);
        anotherAdminMember.setStatus(GroupMemberStatus.ACTIVE);

        // All active members including owner and admins
        List<GroupMember> allActiveMembers = Arrays.asList(ownerMember, adminMember, anotherAdminMember);

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(adminUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(adminUserId, groupId))
                    .thenReturn(Optional.of(adminMember));
            
            // Mock GroupPermissionService - user cannot remove themselves
            when(groupPermissionService.canRemoveMember(GroupMemberRole.ADMIN, GroupMemberRole.ADMIN))
                    .thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, () -> {
                groupMemberService.removeMemberFromGroup(groupId, adminUserId);
            });

            assertEquals("Insufficient permission to remove this member", exception.getMessage());
        }
    }

    /**
     * Helper method to create test user
     */
    private User createTestUser(Integer userId, String email, String fullName) {
        User user = new User();
        user.setUserId(userId);
        user.setUserEmail(email);
        user.setUserFullName(fullName);
        user.setUserIsActive(true);
        return user;
    }

    /**
     * Helper method to create test group
     */
    private Group createTestGroup(Integer groupId, String groupName) {
        Group group = new Group();
        group.setGroupId(groupId);
        group.setGroupName(groupName);
        group.setGroupIsActive(true);
        return group;
    }

    // =============== UPDATE MEMBER ROLE TESTS ===============
    
    @Test
    @DisplayName("updateMemberRole - Should successfully update member to admin when owner updates")
    void updateMemberRole_ShouldSuccessfullyUpdateMemberToAdmin_WhenOwnerUpdates() {
        // Given
        Integer groupId = 1;
        Integer memberUserId = 2;
        Integer ownerUserId = 3;
        User ownerUser = createTestUser(ownerUserId, "owner@example.com", "Owner User");
        User memberUser = createTestUser(memberUserId, "member@example.com", "Member User");
        Group group = createTestGroup(groupId, "Test Group");

        GroupMember ownerMember = new GroupMember();
        ownerMember.setUser(ownerUser);
        ownerMember.setGroup(group);
        ownerMember.setRole(GroupMemberRole.OWNER);
        ownerMember.setStatus(GroupMemberStatus.ACTIVE);

        GroupMember targetMember = new GroupMember();
        targetMember.setUser(memberUser);
        targetMember.setGroup(group);
        targetMember.setRole(GroupMemberRole.MEMBER);
        targetMember.setStatus(GroupMemberStatus.ACTIVE);

        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
        request.setNewRole(GroupMemberRole.ADMIN);

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(ownerUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(ownerUserId, groupId))
                    .thenReturn(Optional.of(ownerMember));
            when(groupMemberRepository.findByUserIdAndGroupId(memberUserId, groupId))
                    .thenReturn(Optional.of(targetMember));
            when(groupPermissionService.canUpdateMemberRole(GroupMemberRole.OWNER, GroupMemberRole.MEMBER, GroupMemberRole.ADMIN))
                    .thenReturn(true);

            // When
            groupMemberService.updateMemberRole(groupId, memberUserId, request);

            // Then
            verify(groupMemberRepository).save(targetMember);
            assertEquals(GroupMemberRole.ADMIN, targetMember.getRole());
        }
    }

    @Test
    @DisplayName("updateMemberRole - Should successfully update admin to member when owner updates") 
    void updateMemberRole_ShouldSuccessfullyUpdateAdminToMember_WhenOwnerUpdates() {
        // Given
        Integer groupId = 1;
        Integer adminUserId = 2;
        Integer ownerUserId = 3;
        User ownerUser = createTestUser(ownerUserId, "owner@example.com", "Owner User");
        User adminUser = createTestUser(adminUserId, "admin@example.com", "Admin User");
        Group group = createTestGroup(groupId, "Test Group");

        GroupMember ownerMember = new GroupMember();
        ownerMember.setUser(ownerUser);
        ownerMember.setGroup(group);
        ownerMember.setRole(GroupMemberRole.OWNER);
        ownerMember.setStatus(GroupMemberStatus.ACTIVE);

        GroupMember targetMember = new GroupMember();
        targetMember.setUser(adminUser);
        targetMember.setGroup(group);
        targetMember.setRole(GroupMemberRole.ADMIN);
        targetMember.setStatus(GroupMemberStatus.ACTIVE);

        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
        request.setNewRole(GroupMemberRole.MEMBER);

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(ownerUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(ownerUserId, groupId))
                    .thenReturn(Optional.of(ownerMember));
            when(groupMemberRepository.findByUserIdAndGroupId(adminUserId, groupId))
                    .thenReturn(Optional.of(targetMember));
            when(groupPermissionService.canUpdateMemberRole(GroupMemberRole.OWNER, GroupMemberRole.ADMIN, GroupMemberRole.MEMBER))
                    .thenReturn(true);

            // When
            groupMemberService.updateMemberRole(groupId, adminUserId, request);

            // Then
            verify(groupMemberRepository).save(targetMember);
            assertEquals(GroupMemberRole.MEMBER, targetMember.getRole());
        }
    }

    @Test
    @DisplayName("Should throw exception when group ID is null")
    void updateMemberRole_ShouldThrowException_WhenGroupIdIsNull() {
        // Given
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
        request.setNewRole(GroupMemberRole.ADMIN);

        // When & Then
        GroupException exception = assertThrows(GroupException.class, () ->
                groupMemberService.updateMemberRole(null, 2, request)
        );
        
        assertEquals("Group ID is required for updating member role", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when member user ID is null")
    void updateMemberRole_ShouldThrowException_WhenMemberUserIdIsNull() {
        // Given
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
        request.setNewRole(GroupMemberRole.ADMIN);

        // When & Then
        GroupException exception = assertThrows(GroupException.class, () ->
                groupMemberService.updateMemberRole(1, null, request)
        );
        
        assertEquals("Member user ID is required for updating member role", exception.getMessage());
    }

    @Test
    @DisplayName("updateMemberRole - Should throw exception when request is null")
    void updateMemberRole_ShouldThrowException_WhenRequestIsNull() {
        // Given
        Integer groupId = 1;
        Integer memberUserId = 2;

        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupMemberService.updateMemberRole(groupId, memberUserId, null));
        assertEquals("New role is required", exception.getMessage());
    }

    @Test
    @DisplayName("updateMemberRole - Should throw exception when new role is null")
    void updateMemberRole_ShouldThrowException_WhenNewRoleIsNull() {
        // Given
        Integer groupId = 1;
        Integer memberUserId = 2;
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
        request.setNewRole(null);

        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupMemberService.updateMemberRole(groupId, memberUserId, request));
        assertEquals("New role is required", exception.getMessage());
    }

    @Test
    @DisplayName("updateMemberRole - Should throw exception when group not found")
    void updateMemberRole_ShouldThrowException_WhenGroupNotFound() {
        // Given
        Integer groupId = 1;
        Integer memberUserId = 2;
        User ownerUser = createTestUser(3, "owner@example.com", "Owner User");
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
        request.setNewRole(GroupMemberRole.ADMIN);

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(ownerUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupMemberService.updateMemberRole(groupId, memberUserId, request));
            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    @DisplayName("updateMemberRole - Should throw exception when group is inactive")
    void updateMemberRole_ShouldThrowException_WhenGroupIsInactive() {
        // Given
        Integer groupId = 1;
        Integer memberUserId = 2;
        User ownerUser = createTestUser(3, "owner@example.com", "Owner User");
        Group inactiveGroup = createTestGroup(groupId, "Inactive Group");
        inactiveGroup.setGroupIsActive(false);
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
        request.setNewRole(GroupMemberRole.ADMIN);

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(ownerUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(inactiveGroup));

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupMemberService.updateMemberRole(groupId, memberUserId, request));
            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    @DisplayName("updateMemberRole - Should throw exception when current user is not owner")
    void updateMemberRole_ShouldThrowException_WhenCurrentUserIsNotOwner() {
        // Given
        Integer groupId = 1;
        Integer memberUserId = 2;
        Integer adminUserId = 3;
        User adminUser = createTestUser(adminUserId, "admin@example.com", "Admin User");
        Group group = createTestGroup(groupId, "Test Group");

        GroupMember adminMember = new GroupMember();
        adminMember.setUser(adminUser);
        adminMember.setGroup(group);
        adminMember.setRole(GroupMemberRole.ADMIN);
        adminMember.setStatus(GroupMemberStatus.ACTIVE);

        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
        request.setNewRole(GroupMemberRole.ADMIN);

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(adminUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(adminUserId, groupId))
                    .thenReturn(Optional.of(adminMember));

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupMemberService.updateMemberRole(groupId, memberUserId, request));
            assertEquals("Only group owner can update member roles", exception.getMessage());
        }
    }

    @Test
    @DisplayName("updateMemberRole - Should throw exception when target member not found")
    void updateMemberRole_ShouldThrowException_WhenTargetMemberNotFound() {
        // Given
        Integer groupId = 1;
        Integer memberUserId = 2;
        Integer ownerUserId = 3;
        User ownerUser = createTestUser(ownerUserId, "owner@example.com", "Owner User");
        Group group = createTestGroup(groupId, "Test Group");

        GroupMember ownerMember = new GroupMember();
        ownerMember.setUser(ownerUser);
        ownerMember.setGroup(group);
        ownerMember.setRole(GroupMemberRole.OWNER);
        ownerMember.setStatus(GroupMemberStatus.ACTIVE);

        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
        request.setNewRole(GroupMemberRole.ADMIN);

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(ownerUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(ownerUserId, groupId))
                    .thenReturn(Optional.of(ownerMember));
            when(groupMemberRepository.findByUserIdAndGroupId(memberUserId, groupId))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupMemberService.updateMemberRole(groupId, memberUserId, request));
            assertEquals("Member not found in this group", exception.getMessage());
        }
    }

    @Test
    @DisplayName("updateMemberRole - Should throw exception when trying to update owner role")
    void updateMemberRole_ShouldThrowException_WhenTryingToUpdateOwnerRole() {
        // Given
        Integer groupId = 1;
        Integer anotherOwnerUserId = 2;
        Integer ownerUserId = 3;
        User ownerUser = createTestUser(ownerUserId, "owner@example.com", "Owner User");
        User anotherOwnerUser = createTestUser(anotherOwnerUserId, "owner2@example.com", "Another Owner");
        Group group = createTestGroup(groupId, "Test Group");

        GroupMember ownerMember = new GroupMember();
        ownerMember.setUser(ownerUser);
        ownerMember.setGroup(group);
        ownerMember.setRole(GroupMemberRole.OWNER);
        ownerMember.setStatus(GroupMemberStatus.ACTIVE);

        GroupMember targetOwnerMember = new GroupMember();
        targetOwnerMember.setUser(anotherOwnerUser);
        targetOwnerMember.setGroup(group);
        targetOwnerMember.setRole(GroupMemberRole.OWNER);
        targetOwnerMember.setStatus(GroupMemberStatus.ACTIVE);

        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
        request.setNewRole(GroupMemberRole.ADMIN);

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(ownerUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(ownerUserId, groupId))
                    .thenReturn(Optional.of(ownerMember));
            when(groupMemberRepository.findByUserIdAndGroupId(anotherOwnerUserId, groupId))
                    .thenReturn(Optional.of(targetOwnerMember));
            when(groupPermissionService.canUpdateMemberRole(GroupMemberRole.OWNER, GroupMemberRole.OWNER, GroupMemberRole.ADMIN))
                    .thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupMemberService.updateMemberRole(groupId, anotherOwnerUserId, request));
            assertEquals("Cannot change owner role", exception.getMessage());
        }
    }

    @Test
    @DisplayName("Should throw exception when trying to promote to owner")
    void updateMemberRole_ShouldThrowException_WhenTryingToPromoteToOwner() {
        // Given
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
        request.setNewRole(GroupMemberRole.OWNER);

        Integer groupId = 1;
        Integer memberUserId = 2;
        Integer ownerUserId = 3;
        User ownerUser = createTestUser(ownerUserId, "owner@example.com", "Owner User");
        User memberUser = createTestUser(memberUserId, "member@example.com", "Member User");
        Group group = createTestGroup(groupId, "Test Group");

        GroupMember ownerMember = new GroupMember();
        ownerMember.setUser(ownerUser);
        ownerMember.setGroup(group);
        ownerMember.setRole(GroupMemberRole.OWNER);
        ownerMember.setStatus(GroupMemberStatus.ACTIVE);

        GroupMember targetMember = new GroupMember();
        targetMember.setUser(memberUser);
        targetMember.setGroup(group);
        targetMember.setRole(GroupMemberRole.MEMBER);
        targetMember.setStatus(GroupMemberStatus.ACTIVE);

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(ownerUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(ownerUserId, groupId))
                    .thenReturn(Optional.of(ownerMember));
            when(groupMemberRepository.findByUserIdAndGroupId(memberUserId, groupId))
                    .thenReturn(Optional.of(targetMember));
            when(groupPermissionService.canUpdateMemberRole(GroupMemberRole.OWNER, GroupMemberRole.MEMBER, GroupMemberRole.OWNER))
                    .thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, () ->
                    groupMemberService.updateMemberRole(groupId, memberUserId, request)
            );
            
            assertEquals("Cannot promote to owner (prevent multiple owners)", exception.getMessage());
        }
    }

    @Test
    @DisplayName("Should throw exception when member already has the role")
    void updateMemberRole_ShouldThrowException_WhenMemberAlreadyHasTheRole() {
        // Given
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
        request.setNewRole(GroupMemberRole.ADMIN);

        Integer groupId = 1;
        Integer memberUserId = 2;
        Integer ownerUserId = 3;
        User ownerUser = createTestUser(ownerUserId, "owner@example.com", "Owner User");
        User memberUser = createTestUser(memberUserId, "member@example.com", "Member User");
        Group group = createTestGroup(groupId, "Test Group");

        GroupMember ownerMember = new GroupMember();
        ownerMember.setUser(ownerUser);
        ownerMember.setGroup(group);
        ownerMember.setRole(GroupMemberRole.OWNER);
        ownerMember.setStatus(GroupMemberStatus.ACTIVE);

        GroupMember targetMember = new GroupMember();
        targetMember.setUser(memberUser);
        targetMember.setGroup(group);
        targetMember.setRole(GroupMemberRole.ADMIN); // Same as request
        targetMember.setStatus(GroupMemberStatus.ACTIVE);

        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(ownerUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(ownerUserId, groupId))
                    .thenReturn(Optional.of(ownerMember));
            when(groupMemberRepository.findByUserIdAndGroupId(memberUserId, groupId))
                    .thenReturn(Optional.of(targetMember));

            // When & Then
            GroupException exception = assertThrows(GroupException.class, () ->
                    groupMemberService.updateMemberRole(groupId, memberUserId, request)
            );
            
            assertEquals("Member already has this role", exception.getMessage());
        }
    }

    // ===== GET PENDING GROUP MEMBERS TESTS =====

    @Test
    @DisplayName("getPendingGroupMembers - Should successfully return pending members when admin requests")
    void getPendingGroupMembers_ShouldSuccessfullyReturnPendingMembers_WhenAdminRequests() {
        // Given
        Integer groupId = 1;
        User currentUser = createTestUser(2, "admin@example.com", "Admin User");
        Group group = createTestGroup(groupId, "Test Group");
        
        GroupMember adminMember = new GroupMember();
        adminMember.setUser(currentUser);
        adminMember.setGroup(group);
        adminMember.setRole(GroupMemberRole.ADMIN);
        adminMember.setStatus(GroupMemberStatus.ACTIVE);

        User pendingUser1 = createTestUser(3, "pending1@example.com", "Pending User 1");
        User pendingUser2 = createTestUser(4, "pending2@example.com", "Pending User 2");

        GroupMember pendingMember1 = new GroupMember();
        pendingMember1.setUser(pendingUser1);
        pendingMember1.setGroup(group);
        pendingMember1.setRole(GroupMemberRole.MEMBER);
        pendingMember1.setStatus(GroupMemberStatus.PENDING_APPROVAL);
        pendingMember1.setJoinDate(LocalDateTime.now().minusDays(2));

        GroupMember pendingMember2 = new GroupMember();
        pendingMember2.setUser(pendingUser2);
        pendingMember2.setGroup(group);
        pendingMember2.setRole(GroupMemberRole.MEMBER);
        pendingMember2.setStatus(GroupMemberStatus.PENDING_APPROVAL);
        pendingMember2.setJoinDate(LocalDateTime.now().minusDays(1));

        List<GroupMember> pendingMembers = Arrays.asList(pendingMember1, pendingMember2);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(adminMember));
            when(groupPermissionService.canViewPendingRequests(GroupMemberRole.ADMIN))
                    .thenReturn(true);
            when(groupMemberRepository.findPendingGroupMembers(
                    groupId, GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(pendingMembers);
            when(groupMemberRepository.countPendingGroupMembers(
                    groupId, GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(2L);

            // When
            PendingGroupMemberListResponse result = groupMemberService.getPendingGroupMembers(groupId);

            // Then
            assertNotNull(result);
            assertEquals(groupId, result.getGroupId());
            assertEquals(group.getGroupName(), result.getGroupName());
            assertEquals(2, result.getTotalPendingCount());
            assertEquals(2, result.getPendingMembers().size());

            // Verify first pending member
            PendingGroupMemberResponse firstPending = result.getPendingMembers().get(0);
            assertEquals(pendingUser1.getUserId(), firstPending.getUserId());
            assertEquals(pendingUser1.getUserFullName(), firstPending.getUserFullName());
            assertEquals(pendingUser1.getUserEmail(), firstPending.getUserEmail());
            assertEquals(pendingMember1.getJoinDate(), firstPending.getRequestedAt());

            verify(groupPermissionService).canViewPendingRequests(GroupMemberRole.ADMIN);
        }
    }

    @Test
    @DisplayName("getPendingGroupMembers - Should throw exception when group ID is null")
    void getPendingGroupMembers_ShouldThrowException_WhenGroupIdIsNull() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupMemberService.getPendingGroupMembers(null));

        assertEquals("Group ID cannot be null", exception.getMessage());
        verifyNoInteractions(groupRepository, groupMemberRepository, groupPermissionService);
    }

    @Test
    @DisplayName("getPendingGroupMembers - Should throw exception when user is regular member")
    void getPendingGroupMembers_ShouldThrowException_WhenUserIsRegularMember() {
        // Given
        Integer groupId = 1;
        User currentUser = createTestUser(1, "member@example.com", "Member User");
        Group group = createTestGroup(groupId, "Test Group");
        
        GroupMember regularMember = new GroupMember();
        regularMember.setUser(currentUser);
        regularMember.setGroup(group);
        regularMember.setRole(GroupMemberRole.MEMBER);
        regularMember.setStatus(GroupMemberStatus.ACTIVE);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(regularMember));
            when(groupPermissionService.canViewPendingRequests(GroupMemberRole.MEMBER))
                    .thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupMemberService.getPendingGroupMembers(groupId));

            assertEquals("You don't have permission to view pending member requests. Only admins and owners can view pending requests.", exception.getMessage());
            verify(groupPermissionService).canViewPendingRequests(GroupMemberRole.MEMBER);
        }
    }

    // ===== ACCEPT GROUP MEMBER REQUEST TESTS =====

    @Test
    @DisplayName("acceptGroupMemberRequest - Should successfully accept request when admin accepts")
    void acceptGroupMemberRequest_ShouldSuccessfullyAcceptRequest_WhenAdminAccepts() {
        // Given
        Integer groupId = 1;
        Integer pendingUserId = 3;
        AcceptGroupMemberRequest request = new AcceptGroupMemberRequest(pendingUserId);
        
        User currentUser = createTestUser(2, "admin@example.com", "Admin User");
        User pendingUser = createTestUser(pendingUserId, "pending@example.com", "Pending User");
        Group group = createTestGroup(groupId, "Test Group");
        
        GroupMember adminMember = new GroupMember();
        adminMember.setUser(currentUser);
        adminMember.setGroup(group);
        adminMember.setRole(GroupMemberRole.ADMIN);
        adminMember.setStatus(GroupMemberStatus.ACTIVE);

        GroupMember pendingMember = new GroupMember();
        pendingMember.setUser(pendingUser);
        pendingMember.setGroup(group);
        pendingMember.setRole(GroupMemberRole.MEMBER);
        pendingMember.setStatus(GroupMemberStatus.PENDING_APPROVAL);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(adminMember));
            when(groupPermissionService.canAcceptGroupMemberRequests(GroupMemberRole.ADMIN))
                    .thenReturn(true);
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    pendingUserId, groupId, GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(Optional.of(pendingMember));

            // When
            groupMemberService.acceptGroupMemberRequest(groupId, request);

            // Then
            verify(groupMemberRepository).save(pendingMember);
            assertEquals(GroupMemberStatus.ACTIVE, pendingMember.getStatus());
            assertEquals(GroupMemberRole.MEMBER, pendingMember.getRole());
            verify(groupPermissionService).canAcceptGroupMemberRequests(GroupMemberRole.ADMIN);
        }
    }

    @Test
    @DisplayName("acceptGroupMemberRequest - Should successfully accept request when owner accepts")
    void acceptGroupMemberRequest_ShouldSuccessfullyAcceptRequest_WhenOwnerAccepts() {
        // Given
        Integer groupId = 1;
        Integer pendingUserId = 3;
        AcceptGroupMemberRequest request = new AcceptGroupMemberRequest(pendingUserId);
        
        User currentUser = createTestUser(2, "owner@example.com", "Owner User");
        User pendingUser = createTestUser(pendingUserId, "pending@example.com", "Pending User");
        Group group = createTestGroup(groupId, "Test Group");
        
        GroupMember ownerMember = new GroupMember();
        ownerMember.setUser(currentUser);
        ownerMember.setGroup(group);
        ownerMember.setRole(GroupMemberRole.OWNER);
        ownerMember.setStatus(GroupMemberStatus.ACTIVE);

        GroupMember pendingMember = new GroupMember();
        pendingMember.setUser(pendingUser);
        pendingMember.setGroup(group);
        pendingMember.setRole(GroupMemberRole.MEMBER);
        pendingMember.setStatus(GroupMemberStatus.PENDING_APPROVAL);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(ownerMember));
            when(groupPermissionService.canAcceptGroupMemberRequests(GroupMemberRole.OWNER))
                    .thenReturn(true);
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    pendingUserId, groupId, GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(Optional.of(pendingMember));

            // When
            groupMemberService.acceptGroupMemberRequest(groupId, request);

            // Then
            verify(groupMemberRepository).save(pendingMember);
            assertEquals(GroupMemberStatus.ACTIVE, pendingMember.getStatus());
            assertEquals(GroupMemberRole.MEMBER, pendingMember.getRole());
        }
    }

    @Test
    @DisplayName("acceptGroupMemberRequest - Should throw exception when group ID is null")
    void acceptGroupMemberRequest_ShouldThrowException_WhenGroupIdIsNull() {
        // Given
        AcceptGroupMemberRequest request = new AcceptGroupMemberRequest(1);

        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupMemberService.acceptGroupMemberRequest(null, request));

        assertEquals("Group ID cannot be null", exception.getMessage());
        verifyNoInteractions(groupRepository, groupMemberRepository, groupPermissionService);
    }

    @Test
    @DisplayName("acceptGroupMemberRequest - Should throw exception when request is null")
    void acceptGroupMemberRequest_ShouldThrowException_WhenRequestIsNull() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupMemberService.acceptGroupMemberRequest(1, null));

        assertEquals("Request cannot be null", exception.getMessage());
        verifyNoInteractions(groupRepository, groupMemberRepository, groupPermissionService);
    }

    @Test
    @DisplayName("acceptGroupMemberRequest - Should throw exception when user is regular member")
    void acceptGroupMemberRequest_ShouldThrowException_WhenUserIsRegularMember() {
        // Given
        Integer groupId = 1;
        AcceptGroupMemberRequest request = new AcceptGroupMemberRequest(3);
        
        User currentUser = createTestUser(2, "member@example.com", "Member User");
        Group group = createTestGroup(groupId, "Test Group");
        
        GroupMember regularMember = new GroupMember();
        regularMember.setUser(currentUser);
        regularMember.setGroup(group);
        regularMember.setRole(GroupMemberRole.MEMBER);
        regularMember.setStatus(GroupMemberStatus.ACTIVE);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(regularMember));
            when(groupPermissionService.canAcceptGroupMemberRequests(GroupMemberRole.MEMBER))
                    .thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupMemberService.acceptGroupMemberRequest(groupId, request));

            assertEquals("You don't have permission to accept member requests. Only admins and owners can accept requests.", exception.getMessage());
            verify(groupPermissionService).canAcceptGroupMemberRequests(GroupMemberRole.MEMBER);
            verify(groupMemberRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("acceptGroupMemberRequest - Should throw exception when no pending request found")
    void acceptGroupMemberRequest_ShouldThrowException_WhenNoPendingRequestFound() {
        // Given
        Integer groupId = 1;
        Integer userId = 3;
        AcceptGroupMemberRequest request = new AcceptGroupMemberRequest(userId);
        
        User currentUser = createTestUser(2, "admin@example.com", "Admin User");
        Group group = createTestGroup(groupId, "Test Group");
        
        GroupMember adminMember = new GroupMember();
        adminMember.setUser(currentUser);
        adminMember.setGroup(group);
        adminMember.setRole(GroupMemberRole.ADMIN);
        adminMember.setStatus(GroupMemberStatus.ACTIVE);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(adminMember));
            when(groupPermissionService.canAcceptGroupMemberRequests(GroupMemberRole.ADMIN))
                    .thenReturn(true);
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    userId, groupId, GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupMemberService.acceptGroupMemberRequest(groupId, request));

            assertEquals("No pending request found for this user", exception.getMessage());
            verify(groupMemberRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("acceptGroupMemberRequest - Should throw exception when pending user account is inactive")
    void acceptGroupMemberRequest_ShouldThrowException_WhenPendingUserAccountIsInactive() {
        // Given
        Integer groupId = 1;
        Integer pendingUserId = 3;
        AcceptGroupMemberRequest request = new AcceptGroupMemberRequest(pendingUserId);
        
        User currentUser = createTestUser(2, "admin@example.com", "Admin User");
        User inactivePendingUser = createTestUser(pendingUserId, "pending@example.com", "Pending User");
        inactivePendingUser.setUserIsActive(false); // Set as inactive
        Group group = createTestGroup(groupId, "Test Group");
        
        GroupMember adminMember = new GroupMember();
        adminMember.setUser(currentUser);
        adminMember.setGroup(group);
        adminMember.setRole(GroupMemberRole.ADMIN);
        adminMember.setStatus(GroupMemberStatus.ACTIVE);

        GroupMember pendingMember = new GroupMember();
        pendingMember.setUser(inactivePendingUser);
        pendingMember.setGroup(group);
        pendingMember.setRole(GroupMemberRole.MEMBER);
        pendingMember.setStatus(GroupMemberStatus.PENDING_APPROVAL);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(adminMember));
            when(groupPermissionService.canAcceptGroupMemberRequests(GroupMemberRole.ADMIN))
                    .thenReturn(true);
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    pendingUserId, groupId, GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(Optional.of(pendingMember));

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupMemberService.acceptGroupMemberRequest(groupId, request));

            assertEquals("User account is no longer active", exception.getMessage());
            verify(groupMemberRepository, never()).save(any());
        }
    }

    // ===== REJECT GROUP MEMBER REQUEST TESTS =====

    @Test
    @DisplayName("rejectGroupMemberRequest - Should successfully reject request when admin rejects")
    void rejectGroupMemberRequest_ShouldSuccessfullyRejectRequest_WhenAdminRejects() {
        // Given
        Integer groupId = 1;
        Integer pendingUserId = 3;
        RejectGroupMemberRequest request = new RejectGroupMemberRequest(pendingUserId);
        
        User currentUser = createTestUser(2, "admin@example.com", "Admin User");
        User pendingUser = createTestUser(pendingUserId, "pending@example.com", "Pending User");
        Group group = createTestGroup(groupId, "Test Group");
        
        GroupMember adminMember = new GroupMember();
        adminMember.setUser(currentUser);
        adminMember.setGroup(group);
        adminMember.setRole(GroupMemberRole.ADMIN);
        adminMember.setStatus(GroupMemberStatus.ACTIVE);

        GroupMember pendingMember = new GroupMember();
        pendingMember.setUser(pendingUser);
        pendingMember.setGroup(group);
        pendingMember.setRole(GroupMemberRole.MEMBER);
        pendingMember.setStatus(GroupMemberStatus.PENDING_APPROVAL);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(adminMember));
            when(groupPermissionService.canRejectGroupMemberRequests(GroupMemberRole.ADMIN))
                    .thenReturn(true);
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    pendingUserId, groupId, GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(Optional.of(pendingMember));

            // When
            groupMemberService.rejectGroupMemberRequest(groupId, request);

            // Then
            verify(groupMemberRepository).save(pendingMember);
            assertEquals(GroupMemberStatus.REJECTED, pendingMember.getStatus());
            verify(groupPermissionService).canRejectGroupMemberRequests(GroupMemberRole.ADMIN);
        }
    }

    @Test
    @DisplayName("rejectGroupMemberRequest - Should successfully reject request when owner rejects")
    void rejectGroupMemberRequest_ShouldSuccessfullyRejectRequest_WhenOwnerRejects() {
        // Given
        Integer groupId = 1;
        Integer pendingUserId = 3;
        RejectGroupMemberRequest request = new RejectGroupMemberRequest(pendingUserId);
        
        User currentUser = createTestUser(2, "owner@example.com", "Owner User");
        User pendingUser = createTestUser(pendingUserId, "pending@example.com", "Pending User");
        Group group = createTestGroup(groupId, "Test Group");
        
        GroupMember ownerMember = new GroupMember();
        ownerMember.setUser(currentUser);
        ownerMember.setGroup(group);
        ownerMember.setRole(GroupMemberRole.OWNER);
        ownerMember.setStatus(GroupMemberStatus.ACTIVE);

        GroupMember pendingMember = new GroupMember();
        pendingMember.setUser(pendingUser);
        pendingMember.setGroup(group);
        pendingMember.setRole(GroupMemberRole.MEMBER);
        pendingMember.setStatus(GroupMemberStatus.PENDING_APPROVAL);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(ownerMember));
            when(groupPermissionService.canRejectGroupMemberRequests(GroupMemberRole.OWNER))
                    .thenReturn(true);
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    pendingUserId, groupId, GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(Optional.of(pendingMember));

            // When
            groupMemberService.rejectGroupMemberRequest(groupId, request);

            // Then
            verify(groupMemberRepository).save(pendingMember);
            assertEquals(GroupMemberStatus.REJECTED, pendingMember.getStatus());
        }
    }

    @Test
    @DisplayName("rejectGroupMemberRequest - Should throw exception when group ID is null")
    void rejectGroupMemberRequest_ShouldThrowException_WhenGroupIdIsNull() {
        // Given
        RejectGroupMemberRequest request = new RejectGroupMemberRequest(1);

        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupMemberService.rejectGroupMemberRequest(null, request));

        assertEquals("Group ID cannot be null", exception.getMessage());
        verifyNoInteractions(groupRepository, groupMemberRepository, groupPermissionService);
    }

    @Test
    @DisplayName("rejectGroupMemberRequest - Should throw exception when request is null")
    void rejectGroupMemberRequest_ShouldThrowException_WhenRequestIsNull() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class,
                () -> groupMemberService.rejectGroupMemberRequest(1, null));

        assertEquals("Request cannot be null", exception.getMessage());
        verifyNoInteractions(groupRepository, groupMemberRepository, groupPermissionService);
    }

    @Test
    @DisplayName("rejectGroupMemberRequest - Should throw exception when user is regular member")
    void rejectGroupMemberRequest_ShouldThrowException_WhenUserIsRegularMember() {
        // Given
        Integer groupId = 1;
        RejectGroupMemberRequest request = new RejectGroupMemberRequest(3);
        
        User currentUser = createTestUser(2, "member@example.com", "Member User");
        Group group = createTestGroup(groupId, "Test Group");
        
        GroupMember regularMember = new GroupMember();
        regularMember.setUser(currentUser);
        regularMember.setGroup(group);
        regularMember.setRole(GroupMemberRole.MEMBER);
        regularMember.setStatus(GroupMemberStatus.ACTIVE);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(regularMember));
            when(groupPermissionService.canRejectGroupMemberRequests(GroupMemberRole.MEMBER))
                    .thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupMemberService.rejectGroupMemberRequest(groupId, request));

            assertEquals("You don't have permission to reject member requests. Only admins and owners can reject requests.", exception.getMessage());
            verify(groupPermissionService).canRejectGroupMemberRequests(GroupMemberRole.MEMBER);
            verify(groupMemberRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("rejectGroupMemberRequest - Should throw exception when no pending request found")
    void rejectGroupMemberRequest_ShouldThrowException_WhenNoPendingRequestFound() {
        // Given
        Integer groupId = 1;
        Integer userId = 3;
        RejectGroupMemberRequest request = new RejectGroupMemberRequest(userId);
        
        User currentUser = createTestUser(2, "admin@example.com", "Admin User");
        Group group = createTestGroup(groupId, "Test Group");
        
        GroupMember adminMember = new GroupMember();
        adminMember.setUser(currentUser);
        adminMember.setGroup(group);
        adminMember.setRole(GroupMemberRole.ADMIN);
        adminMember.setStatus(GroupMemberStatus.ACTIVE);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(adminMember));
            when(groupPermissionService.canRejectGroupMemberRequests(GroupMemberRole.ADMIN))
                    .thenReturn(true);
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    userId, groupId, GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupMemberService.rejectGroupMemberRequest(groupId, request));

            assertEquals("No pending request found for this user", exception.getMessage());
            verify(groupMemberRepository, never()).save(any());
        }
    }
} 