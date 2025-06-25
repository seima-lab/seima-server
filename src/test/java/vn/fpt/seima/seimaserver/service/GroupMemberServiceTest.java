package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberListResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberResponse;
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

        testGroupLeader = createGroupMember(1, testGroup, testLeader, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE);
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
            when(groupMemberRepository.findGroupLeader(groupId, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE))
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
            assertEquals(GroupMemberRole.ADMIN, leader.getRole());

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
            when(groupMemberRepository.findGroupLeader(groupId, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class,
                    () -> groupMemberService.getActiveGroupMembers(groupId));

            assertEquals("Group leader not found for group ID: " + groupId, exception.getMessage());
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
            when(groupMemberRepository.findGroupLeader(groupId, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE))
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
        List<GroupMember> allMembers = Arrays.asList(testGroupLeader);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testLeader);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    testLeader.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(true);
            when(groupMemberRepository.findGroupLeader(groupId, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupLeader));
            when(groupMemberRepository.findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(allMembers);

            // When
            GroupMemberListResponse result = groupMemberService.getActiveGroupMembers(groupId);

            // Then
            assertEquals(1, result.getTotalMembersCount());
            assertEquals(testLeader.getUserId(), result.getGroupLeader().getUserId());
            assertEquals(GroupMemberRole.ADMIN, result.getCurrentUserRole());
            assertTrue(result.getMembers().isEmpty());
        }
    }

    @Test
    void getActiveGroupMembers_ShouldFilterOutInactiveUsers() {
        // Given
        Integer groupId = 1;
        
        // Create inactive user
        User inactiveUser = createUser(5, "Inactive User", false);
        GroupMember inactiveGroupMember = createGroupMember(4, testGroup, inactiveUser, GroupMemberRole.MEMBER, GroupMemberStatus.ACTIVE);
        
        // List includes both active and inactive users
        List<GroupMember> allMembers = Arrays.asList(testGroupLeader, testGroupMember1, testGroupMember2, inactiveGroupMember);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(testCurrentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    testCurrentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(true);
            when(groupMemberRepository.findGroupLeader(groupId, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(testGroupLeader));
            when(groupMemberRepository.findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(allMembers);

            // When
            GroupMemberListResponse result = groupMemberService.getActiveGroupMembers(groupId);

            // Then
            assertNotNull(result);
            // Should only count active users (3 active + 1 inactive = 3 in result)
            assertEquals(3, result.getTotalMembersCount());
            
            // Verify leader is returned correctly (leader is active)
            GroupMemberResponse leader = result.getGroupLeader();
            assertNotNull(leader);
            assertEquals(testLeader.getUserId(), leader.getUserId());
            assertEquals(testLeader.getUserFullName(), leader.getUserFullName());
            assertEquals(GroupMemberRole.ADMIN, leader.getRole());

            // Verify members list only contains active users (should not include leader or inactive user)
            List<GroupMemberResponse> members = result.getMembers();
            assertNotNull(members);
            assertEquals(2, members.size()); // Only 2 active members (excluding leader and inactive user)
            
            // Verify inactive user is not included
            assertFalse(members.stream().anyMatch(member -> 
                member.getUserId().equals(inactiveUser.getUserId())));
            
            // Verify leader is not included in members list
            assertFalse(members.stream().anyMatch(member -> 
                member.getUserId().equals(testLeader.getUserId())));
                
            // Verify all returned members are from active users
            assertTrue(members.stream().allMatch(member -> 
                member.getUserId().equals(testCurrentUser.getUserId()) || 
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
} 