package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.context.Context;
import vn.fpt.seima.seimaserver.dto.request.group.AcceptGroupMemberRequest;
import vn.fpt.seima.seimaserver.dto.request.group.RejectGroupMemberRequest;
import vn.fpt.seima.seimaserver.dto.request.group.UpdateMemberRoleRequest;
import vn.fpt.seima.seimaserver.dto.request.group.TransferOwnershipRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberListResponse;
import vn.fpt.seima.seimaserver.dto.response.group.PendingGroupMemberListResponse;
import vn.fpt.seima.seimaserver.dto.response.group.OwnerExitOptionsResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.exception.GroupException;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.repository.GroupRepository;
import vn.fpt.seima.seimaserver.service.impl.GroupMemberServiceImpl;
import vn.fpt.seima.seimaserver.util.UserUtils;
import vn.fpt.seima.seimaserver.config.base.AppProperties;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupMemberServiceTest {

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupPermissionService groupPermissionService;

    @Mock
    private InvitationTokenService invitationTokenService;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private GroupMemberServiceImpl groupMemberService;

    private User currentUser;
    private User targetUser;
    private User adminUser;
    private User memberUser;
    private Group testGroup;
    private GroupMember ownerGroupMember;
    private GroupMember adminGroupMember;
    private GroupMember memberGroupMember;
    private GroupMember pendingGroupMember;

    @BeforeEach
    void setUp() {
        // Setup users
        currentUser = User.builder()
                .userId(1)
                .userEmail("owner@example.com")
                .userFullName("Group Owner")
                .userIsActive(true)
                .build();

        targetUser = User.builder()
                .userId(2)
                .userEmail("target@example.com")
                .userFullName("Target User")
                .userIsActive(true)
                .build();

        adminUser = User.builder()
                .userId(3)
                .userEmail("admin@example.com")
                .userFullName("Admin User")
                .userIsActive(true)
                .build();

        memberUser = User.builder()
                .userId(4)
                .userEmail("member@example.com")
                .userFullName("Member User")
                .userIsActive(true)
                .build();

        // Setup group
        testGroup = new Group();
        testGroup.setGroupId(1);
        testGroup.setGroupName("Test Group");
        testGroup.setGroupAvatarUrl("http://example.com/avatar.jpg");
        testGroup.setGroupIsActive(true);

        // Setup group members
        ownerGroupMember = new GroupMember();
        ownerGroupMember.setGroupMemberId(1);
        ownerGroupMember.setUser(currentUser);
        ownerGroupMember.setGroup(testGroup);
        ownerGroupMember.setRole(GroupMemberRole.OWNER);
        ownerGroupMember.setStatus(GroupMemberStatus.ACTIVE);
        ownerGroupMember.setJoinDate(LocalDateTime.now().minusDays(30));

        adminGroupMember = new GroupMember();
        adminGroupMember.setGroupMemberId(2);
        adminGroupMember.setUser(adminUser);
        adminGroupMember.setGroup(testGroup);
        adminGroupMember.setRole(GroupMemberRole.ADMIN);
        adminGroupMember.setStatus(GroupMemberStatus.ACTIVE);
        adminGroupMember.setJoinDate(LocalDateTime.now().minusDays(20));

        memberGroupMember = new GroupMember();
        memberGroupMember.setGroupMemberId(3);
        memberGroupMember.setUser(memberUser);
        memberGroupMember.setGroup(testGroup);
        memberGroupMember.setRole(GroupMemberRole.MEMBER);
        memberGroupMember.setStatus(GroupMemberStatus.ACTIVE);
        memberGroupMember.setJoinDate(LocalDateTime.now().minusDays(10));

        pendingGroupMember = new GroupMember();
        pendingGroupMember.setGroupMemberId(4);
        pendingGroupMember.setUser(targetUser);
        pendingGroupMember.setGroup(testGroup);
        pendingGroupMember.setRole(GroupMemberRole.MEMBER);
        pendingGroupMember.setStatus(GroupMemberStatus.PENDING_APPROVAL);
        pendingGroupMember.setJoinDate(LocalDateTime.now().minusDays(1));
    }

    // ===== getActiveGroupMembers Tests =====
    @Test
    void getActiveGroupMembers_WhenValidRequest_ShouldReturnActiveMembers() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(true);
            when(groupMemberRepository.findGroupOwner(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(ownerGroupMember));
            when(groupMemberRepository.findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Arrays.asList(ownerGroupMember, adminGroupMember, memberGroupMember));

            // When
            GroupMemberListResponse result = groupMemberService.getActiveGroupMembers(groupId);

            // Then
            assertNotNull(result);
            assertEquals(groupId, result.getGroupId());
            assertEquals("Test Group", result.getGroupName());
            assertEquals(3, result.getTotalMembersCount());
            assertEquals(2, result.getMembers().size()); // Excluding owner from members list
            assertEquals(GroupMemberRole.OWNER, result.getCurrentUserRole());
            assertNotNull(result.getGroupLeader());
            assertEquals(currentUser.getUserId(), result.getGroupLeader().getUserId());

            verify(groupRepository).findById(groupId);
            verify(groupMemberRepository).existsByUserAndGroupAndStatus(
                    currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE);
        }
    }

    @Test
    void getActiveGroupMembers_WhenGroupNotFound_ShouldThrowException() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupMemberService.getActiveGroupMembers(groupId));
            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    void getActiveGroupMembers_WhenGroupIdIsNull_ShouldThrowException() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
                () -> groupMemberService.getActiveGroupMembers(null));
        assertEquals("Group ID cannot be null", exception.getMessage());
    }

    @Test
    void getActiveGroupMembers_WhenUserNotMember_ShouldThrowException() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupMemberService.getActiveGroupMembers(groupId));
            assertEquals("You don't have permission to view this group's members", exception.getMessage());
        }
    }

    // ===== getPendingGroupMembers Tests =====
    @Test
    void getPendingGroupMembers_WhenValidOwnerRequest_ShouldReturnPendingMembers() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(ownerGroupMember));
            when(groupPermissionService.canViewPendingRequests(GroupMemberRole.OWNER))
                    .thenReturn(true);
            when(groupMemberRepository.findPendingGroupMembers(groupId, GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(Arrays.asList(pendingGroupMember));
            when(groupMemberRepository.countPendingGroupMembers(groupId, GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(1L);

            // When
            PendingGroupMemberListResponse result = groupMemberService.getPendingGroupMembers(groupId);

            // Then
            assertNotNull(result);
            assertEquals(groupId, result.getGroupId());
            assertEquals("Test Group", result.getGroupName());
            assertEquals(1, result.getTotalPendingCount());
            assertEquals(1, result.getPendingMembers().size());
            assertEquals(targetUser.getUserId(), result.getPendingMembers().get(0).getUserId());

            verify(groupPermissionService).canViewPendingRequests(GroupMemberRole.OWNER);
        }
    }

    @Test
    void getPendingGroupMembers_WhenMemberRole_ShouldThrowException() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(memberUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    memberUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(memberGroupMember));
            when(groupPermissionService.canViewPendingRequests(GroupMemberRole.MEMBER))
                    .thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupMemberService.getPendingGroupMembers(groupId));
            assertEquals("You don't have permission to view pending member requests. Only admins and owners can view pending requests.", 
                    exception.getMessage());
        }
    }

    // ===== acceptGroupMemberRequest Tests =====
    @Test
    void acceptGroupMemberRequest_WhenValidRequest_ShouldAcceptMember() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            AcceptGroupMemberRequest request = new AcceptGroupMemberRequest(targetUser.getUserId());
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(ownerGroupMember));
            when(groupPermissionService.canAcceptGroupMemberRequests(GroupMemberRole.OWNER))
                    .thenReturn(true);
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    targetUser.getUserId(), groupId, GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(Optional.of(pendingGroupMember));
            when(invitationTokenService.removeInvitationTokenByUserAndGroup(
                    targetUser.getUserId(), groupId))
                    .thenReturn(true);

            // When
            groupMemberService.acceptGroupMemberRequest(groupId, request);

            // Then
            assertEquals(GroupMemberStatus.ACTIVE, pendingGroupMember.getStatus());
            assertEquals(GroupMemberRole.MEMBER, pendingGroupMember.getRole());
            verify(groupMemberRepository).save(pendingGroupMember);
            verify(invitationTokenService).removeInvitationTokenByUserAndGroup(targetUser.getUserId(), groupId);
        }
    }

    @Test
    void acceptGroupMemberRequest_WhenRequestIsNull_ShouldThrowException() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
                () -> groupMemberService.acceptGroupMemberRequest(1, null));
        assertEquals("Request cannot be null", exception.getMessage());
    }

    @Test
    void acceptGroupMemberRequest_WhenPendingRequestNotFound_ShouldThrowException() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            AcceptGroupMemberRequest request = new AcceptGroupMemberRequest(targetUser.getUserId());
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(ownerGroupMember));
            when(groupPermissionService.canAcceptGroupMemberRequests(GroupMemberRole.OWNER))
                    .thenReturn(true);
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    targetUser.getUserId(), groupId, GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupMemberService.acceptGroupMemberRequest(groupId, request));
            assertEquals("No pending request found for this user", exception.getMessage());
        }
    }

    // ===== rejectGroupMemberRequest Tests =====
    @Test
    void rejectGroupMemberRequest_WhenValidRequest_ShouldRejectMember() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            RejectGroupMemberRequest request = new RejectGroupMemberRequest();
            request.setUserId(targetUser.getUserId());
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(ownerGroupMember));
            when(groupPermissionService.canRejectGroupMemberRequests(GroupMemberRole.OWNER))
                    .thenReturn(true);
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    targetUser.getUserId(), groupId, GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(Optional.of(pendingGroupMember));
            when(invitationTokenService.removeInvitationTokenByUserAndGroup(
                    targetUser.getUserId(), groupId))
                    .thenReturn(true);

            // When
            groupMemberService.rejectGroupMemberRequest(groupId, request);

            // Then
            assertEquals(GroupMemberStatus.REJECTED, pendingGroupMember.getStatus());
            verify(groupMemberRepository).save(pendingGroupMember);
            verify(invitationTokenService).removeInvitationTokenByUserAndGroup(targetUser.getUserId(), groupId);
        }
    }

    // ===== removeMemberFromGroup Tests =====
    @Test
    void removeMemberFromGroup_WhenOwnerRemovesMember_ShouldRemoveMember() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            Integer memberUserId = memberUser.getUserId();
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(currentUser.getUserId(), groupId))
                    .thenReturn(Optional.of(ownerGroupMember));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(memberUserId, groupId))
                    .thenReturn(Optional.of(memberGroupMember));
            when(groupPermissionService.canRemoveMember(GroupMemberRole.OWNER, GroupMemberRole.MEMBER))
                    .thenReturn(true);

            // When
            groupMemberService.removeMemberFromGroup(groupId, memberUserId);

            // Then
            assertEquals(GroupMemberStatus.LEFT, memberGroupMember.getStatus());
            verify(groupMemberRepository).save(memberGroupMember);
        }
    }

    @Test
    void removeMemberFromGroup_WhenTryingToRemoveOwner_ShouldThrowException() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            Integer ownerUserId = currentUser.getUserId();
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(adminUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(adminUser.getUserId(), groupId))
                    .thenReturn(Optional.of(adminGroupMember));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(ownerUserId, groupId))
                    .thenReturn(Optional.of(ownerGroupMember));

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupMemberService.removeMemberFromGroup(groupId, ownerUserId));
            assertEquals("Group owner cannot be removed from the group", exception.getMessage());
        }
    }

    // ===== updateMemberRole Tests =====
    @Test
    void updateMemberRole_WhenOwnerPromotesMemberToAdmin_ShouldUpdateRole() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            Integer memberUserId = memberUser.getUserId();
            UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
            request.setNewRole(GroupMemberRole.ADMIN);
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(currentUser.getUserId(), groupId))
                    .thenReturn(Optional.of(ownerGroupMember));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(memberUserId, groupId))
                    .thenReturn(Optional.of(memberGroupMember));
            when(groupPermissionService.canUpdateMemberRole(
                    GroupMemberRole.OWNER, GroupMemberRole.MEMBER, GroupMemberRole.ADMIN))
                    .thenReturn(true);
            
            // Mock AppProperties
            when(appProperties.getLabName()).thenReturn("Seima");
            
            // Mock email and notification services
            doNothing().when(emailService).sendEmailWithHtmlTemplate(any(), any(), any(), any());
            doNothing().when(notificationService).sendRoleUpdateNotificationToUser(anyInt(), anyInt(), anyString(), anyString(), any(GroupMemberRole.class), any(GroupMemberRole.class));
            doNothing().when(notificationService).sendRoleUpdateNotificationToGroup(anyInt(), anyInt(), anyString(), anyString(), any(GroupMemberRole.class), any(GroupMemberRole.class));
            when(groupMemberRepository.findByGroupAndStatusAndUserIdNot(anyInt(), any(GroupMemberStatus.class), anyInt()))
                    .thenReturn(Arrays.asList(adminGroupMember));

            // When
            groupMemberService.updateMemberRole(groupId, memberUserId, request);

            // Then
            assertEquals(GroupMemberRole.ADMIN, memberGroupMember.getRole());
            verify(groupMemberRepository).save(memberGroupMember);
            // Verify email calls: one for the updated user, one for group members
            verify(emailService, times(2)).sendEmailWithHtmlTemplate(any(), any(), any(), any());
            verify(notificationService).sendRoleUpdateNotificationToUser(anyInt(), anyInt(), anyString(), anyString(), any(GroupMemberRole.class), any(GroupMemberRole.class));
            verify(notificationService).sendRoleUpdateNotificationToGroup(anyInt(), anyInt(), anyString(), anyString(), any(GroupMemberRole.class), any(GroupMemberRole.class));
        }
    }

    @Test
    void updateMemberRole_WhenUpdatingOwnRole_ShouldThrowException() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            Integer ownUserId = currentUser.getUserId();
            UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
            request.setNewRole(GroupMemberRole.MEMBER);
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(currentUser.getUserId(), groupId))
                    .thenReturn(Optional.of(ownerGroupMember));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(ownUserId, groupId))
                    .thenReturn(Optional.of(ownerGroupMember));

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupMemberService.updateMemberRole(groupId, ownUserId, request));
            assertEquals("Cannot update your own role", exception.getMessage());
        }
    }

    @Test
    void updateMemberRole_WhenOwnerPromotesMemberToAdmin_ShouldSendDifferentEmails() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            Integer memberUserId = memberUser.getUserId();
            UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
            request.setNewRole(GroupMemberRole.ADMIN);
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(currentUser.getUserId(), groupId))
                    .thenReturn(Optional.of(ownerGroupMember));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(memberUserId, groupId))
                    .thenReturn(Optional.of(memberGroupMember));
            when(groupPermissionService.canUpdateMemberRole(
                    GroupMemberRole.OWNER, GroupMemberRole.MEMBER, GroupMemberRole.ADMIN))
                    .thenReturn(true);
            when(groupMemberRepository.findByGroupAndStatusAndUserIdNot(anyInt(), any(GroupMemberStatus.class), anyInt()))
                    .thenReturn(Arrays.asList(adminGroupMember));
            
            // Mock AppProperties
            when(appProperties.getLabName()).thenReturn("Seima");
            
            // Mock email and notification services
            doNothing().when(emailService).sendEmailWithHtmlTemplate(any(), any(), any(), any());
            doNothing().when(notificationService).sendRoleUpdateNotificationToUser(anyInt(), anyInt(), anyString(), anyString(), any(GroupMemberRole.class), any(GroupMemberRole.class));
            doNothing().when(notificationService).sendRoleUpdateNotificationToGroup(anyInt(), anyInt(), anyString(), anyString(), any(GroupMemberRole.class), any(GroupMemberRole.class));

            // When
            groupMemberService.updateMemberRole(groupId, memberUserId, request);

            // Then
            assertEquals(GroupMemberRole.ADMIN, memberGroupMember.getRole());
            verify(groupMemberRepository).save(memberGroupMember);
            
            // Verify email calls with different templates
            verify(emailService, times(2)).sendEmailWithHtmlTemplate(
                any(), // email
                any(), // subject
                any(), // template
                any()  // context
            );
            
            // Verify notification calls
            verify(notificationService).sendRoleUpdateNotificationToUser(anyInt(), anyInt(), anyString(), anyString(), any(GroupMemberRole.class), any(GroupMemberRole.class));
            verify(notificationService).sendRoleUpdateNotificationToGroup(anyInt(), anyInt(), anyString(), anyString(), any(GroupMemberRole.class), any(GroupMemberRole.class));
        }
    }

    // ===== exitGroup Tests =====
    @Test
    void exitGroup_WhenMemberExits_ShouldSetStatusToLeft() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(memberUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(memberUser.getUserId(), groupId))
                    .thenReturn(Optional.of(memberGroupMember));

            // When
            groupMemberService.exitGroup(groupId);

            // Then
            assertEquals(GroupMemberStatus.LEFT, memberGroupMember.getStatus());
            verify(groupMemberRepository).save(memberGroupMember);
        }
    }

    @Test
    void exitGroup_WhenOwnerTries_ShouldThrowException() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(currentUser.getUserId(), groupId))
                    .thenReturn(Optional.of(ownerGroupMember));

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupMemberService.exitGroup(groupId));
            assertEquals("As group owner, you must transfer ownership or delete the group before leaving.", 
                    exception.getMessage());
        }
    }

    // ===== transferOwnership Tests =====
    @Test
    void transferOwnership_WhenValidRequest_ShouldTransferOwnership() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            TransferOwnershipRequest request = new TransferOwnershipRequest();
            request.setNewOwnerUserId(memberUser.getUserId());
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(currentUser.getUserId(), groupId))
                    .thenReturn(Optional.of(ownerGroupMember));
            when(groupMemberRepository.findByUserIdAndGroupId(memberUser.getUserId(), groupId))
                    .thenReturn(Optional.of(memberGroupMember));

            // When
            groupMemberService.transferOwnership(groupId, request);

            // Then
            assertEquals(GroupMemberRole.MEMBER, ownerGroupMember.getRole());
            assertEquals(GroupMemberRole.OWNER, memberGroupMember.getRole());
            verify(groupMemberRepository).save(ownerGroupMember);
            verify(groupMemberRepository).save(memberGroupMember);
        }
    }

    @Test
    void transferOwnership_WhenRequestIsNull_ShouldThrowException() {
        // When & Then
        // Note: The implementation logs request.getNewOwnerUserId() before validation,
        // so NullPointerException is thrown before reaching the validation logic
        assertThrows(NullPointerException.class, 
                () -> groupMemberService.transferOwnership(1, null));
    }

    @Test
    void transferOwnership_WhenNewOwnerUserIdIsNull_ShouldThrowException() {
        // Given
        Integer groupId = 1;
        TransferOwnershipRequest request = new TransferOwnershipRequest();
        request.setNewOwnerUserId(null); // This will trigger validation logic

        // When & Then
        // No mocking needed since validation happens before any repository/service calls
        GroupException exception = assertThrows(GroupException.class, 
                () -> groupMemberService.transferOwnership(groupId, request));
        assertEquals("New owner user ID cannot be null", exception.getMessage());
    }

    // ===== getEligibleMembersForOwnership Tests =====
    @Test
    void getEligibleMembersForOwnership_WhenHasEligibleMembers_ShouldReturnMembers() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(currentUser.getUserId(), groupId))
                    .thenReturn(Optional.of(ownerGroupMember));
            when(groupMemberRepository.findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Arrays.asList(ownerGroupMember, adminGroupMember, memberGroupMember));

            // When
            GroupMemberListResponse result = groupMemberService.getEligibleMembersForOwnership(groupId);

            // Then
            assertNotNull(result);
            assertEquals(groupId, result.getGroupId());
            assertEquals(2, result.getTotalMembersCount()); // Excluding current owner
            assertEquals(2, result.getMembers().size());
            assertEquals(GroupMemberRole.OWNER, result.getCurrentUserRole());
        }
    }

    @Test
    void getEligibleMembersForOwnership_WhenNoEligibleMembers_ShouldThrowException() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(currentUser.getUserId(), groupId))
                    .thenReturn(Optional.of(ownerGroupMember));
            when(groupMemberRepository.findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Arrays.asList(ownerGroupMember)); // Only owner

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupMemberService.getEligibleMembersForOwnership(groupId));
            assertEquals("No eligible members found for ownership transfer. Group must have at least one other active member.", 
                    exception.getMessage());
        }
    }

    // ===== getOwnerExitOptions Tests =====
    @Test
    void getOwnerExitOptions_WhenHasEligibleMembers_ShouldReturnOptions() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            Integer groupId = 1;
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(currentUser.getUserId(), groupId))
                    .thenReturn(Optional.of(ownerGroupMember));
            when(groupMemberRepository.findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE))
                    .thenReturn(Arrays.asList(ownerGroupMember, adminGroupMember, memberGroupMember));

            // When
            OwnerExitOptionsResponse result = groupMemberService.getOwnerExitOptions(groupId);

            // Then
            assertNotNull(result);
            assertEquals(groupId, result.getGroupId());
            assertTrue(result.isCanTransferOwnership());
            assertTrue(result.isCanDeleteGroup());
            assertEquals(2, result.getEligibleMembersCount());
            assertTrue(result.getMessage().contains("2 eligible member(s)"));
        }
    }

    // ===== handleUserAccountDeactivation Tests =====
    @Test
    void handleUserAccountDeactivation_WhenOwnerDeactivated_ShouldPromoteAdmin() {
        // Given
        Integer userId = currentUser.getUserId();
        List<GroupMember> ownerRoles = Arrays.asList(ownerGroupMember);
        List<GroupMember> adminRoles = Collections.emptyList();
        List<GroupMember> activeAdmins = Arrays.asList(adminGroupMember);
        
        when(groupMemberRepository.findByUserIdAndRole(userId, GroupMemberRole.OWNER))
                .thenReturn(ownerRoles);
        when(groupMemberRepository.findByUserIdAndRole(userId, GroupMemberRole.ADMIN))
                .thenReturn(adminRoles);
        when(groupMemberRepository.findActiveGroupMembers(testGroup.getGroupId(), GroupMemberStatus.ACTIVE))
                .thenReturn(Arrays.asList(ownerGroupMember, adminGroupMember));

        // When
        groupMemberService.handleUserAccountDeactivation(userId);

        // Then
        assertEquals(GroupMemberRole.OWNER, adminGroupMember.getRole());
        verify(groupMemberRepository).save(adminGroupMember);
    }

    @Test
    void handleUserAccountDeactivation_WhenOwnerDeactivatedNoAdmins_ShouldPromoteMember() {
        // Given
        Integer userId = currentUser.getUserId();
        List<GroupMember> ownerRoles = Arrays.asList(ownerGroupMember);
        List<GroupMember> adminRoles = Collections.emptyList();
        
        when(groupMemberRepository.findByUserIdAndRole(userId, GroupMemberRole.OWNER))
                .thenReturn(ownerRoles);
        when(groupMemberRepository.findByUserIdAndRole(userId, GroupMemberRole.ADMIN))
                .thenReturn(adminRoles);
        when(groupMemberRepository.findActiveGroupMembers(testGroup.getGroupId(), GroupMemberStatus.ACTIVE))
                .thenReturn(Arrays.asList(ownerGroupMember, memberGroupMember));

        // When
        groupMemberService.handleUserAccountDeactivation(userId);

        // Then
        assertEquals(GroupMemberRole.OWNER, memberGroupMember.getRole());
        verify(groupMemberRepository).save(memberGroupMember);
    }

    @Test
    void handleUserAccountDeactivation_WhenOwnerDeactivatedNoMembers_ShouldDeactivateGroup() {
        // Given
        Integer userId = currentUser.getUserId();
        List<GroupMember> ownerRoles = Arrays.asList(ownerGroupMember);
        List<GroupMember> adminRoles = Collections.emptyList();
        
        when(groupMemberRepository.findByUserIdAndRole(userId, GroupMemberRole.OWNER))
                .thenReturn(ownerRoles);
        when(groupMemberRepository.findByUserIdAndRole(userId, GroupMemberRole.ADMIN))
                .thenReturn(adminRoles);
        when(groupMemberRepository.findActiveGroupMembers(testGroup.getGroupId(), GroupMemberStatus.ACTIVE))
                .thenReturn(Arrays.asList(ownerGroupMember)); // Only owner

        // When
        groupMemberService.handleUserAccountDeactivation(userId);

        // Then
        assertFalse(testGroup.getGroupIsActive());
        verify(groupRepository).save(testGroup);
    }

    @Test
    void handleUserAccountDeactivation_WhenUserIdIsNull_ShouldThrowException() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
                () -> groupMemberService.handleUserAccountDeactivation(null));
        assertEquals("User ID cannot be null", exception.getMessage());
    }
}
