package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.context.Context;
import vn.fpt.seima.seimaserver.config.base.AppProperties;
import vn.fpt.seima.seimaserver.dto.request.group.EmailInvitationRequest;
import vn.fpt.seima.seimaserver.dto.request.group.InvitationTokenData;
import vn.fpt.seima.seimaserver.dto.response.group.*;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.exception.GroupException;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.repository.GroupRepository;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.impl.GroupInvitationServiceImpl;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupInvitationServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private GroupPermissionService groupPermissionService;

    @Mock
    private GroupValidationService groupValidationService;

    @Mock
    private BranchLinkService branchLinkService;

    @Mock
    private AppProperties appProperties;

    @Mock
    private InvitationTokenService invitationTokenService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private GroupInvitationServiceImpl groupInvitationService;

    private User currentUser;
    private User targetUser;
    private Group testGroup;
    private EmailInvitationRequest request;

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

        // Setup group
        testGroup = new Group();
        testGroup.setGroupId(1);
        testGroup.setGroupName("Test Group");
        testGroup.setGroupAvatarUrl("http://example.com/avatar.jpg");
        testGroup.setGroupIsActive(true);

        // Setup request
        request = new EmailInvitationRequest();
        request.setGroupId(1);
        request.setEmail("target@example.com");
    }

    @Test
    void sendEmailInvitation_WhenValidRequest_ShouldSendInvitation() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(request.getGroupId())).thenReturn(Optional.of(testGroup));
            when(userRepository.findByUserEmailAndUserIsActiveTrue(request.getEmail()))
                    .thenReturn(Optional.of(targetUser));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), request.getGroupId()))
                    .thenReturn(Optional.of(createOwnerGroupMember()));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.OWNER))
                    .thenReturn(true);
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), request.getGroupId(), GroupMemberStatus.ACTIVE))
                    .thenReturn(false);
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), request.getGroupId(), GroupMemberStatus.INVITED))
                    .thenReturn(false);
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), request.getGroupId(), GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(false);
            
            // Mock validation service to not throw exception
            doNothing().when(groupValidationService).validateUserCanJoinGroup(targetUser.getUserId(), request.getGroupId());
            
            // Mock email service
            doNothing().when(emailService).sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any());
            
            // Mock invitation token service
            when(invitationTokenService.createInvitationToken(any())).thenReturn("test-token");
            
            // Mock app properties
            AppProperties.Client mockClient = new AppProperties.Client();
            mockClient.setBaseUrl("http://example.com");
            when(appProperties.getClient()).thenReturn(mockClient);

            // When
            EmailInvitationResponse response = groupInvitationService.sendEmailInvitation(request);

            // Then
            assertNotNull(response);
            assertEquals(request.getGroupId(), response.getGroupId());
            assertEquals(testGroup.getGroupName(), response.getGroupName());
            assertEquals(request.getEmail(), response.getInvitedEmail());
            assertTrue(response.isUserExists());
            assertTrue(response.isEmailSent());
            
            verify(groupValidationService).validateUserCanJoinGroup(targetUser.getUserId(), request.getGroupId());
            verify(groupMemberRepository).save(any(GroupMember.class));
            verify(invitationTokenService).createInvitationToken(any());
        }
    }

    @Test
    void sendEmailInvitation_WhenValidationFails_ShouldThrowException() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(request.getGroupId())).thenReturn(Optional.of(testGroup));
            when(userRepository.findByUserEmailAndUserIsActiveTrue(request.getEmail()))
                    .thenReturn(Optional.of(targetUser));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), request.getGroupId()))
                    .thenReturn(Optional.of(createOwnerGroupMember()));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.OWNER))
                    .thenReturn(true);
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), request.getGroupId(), GroupMemberStatus.ACTIVE))
                    .thenReturn(false);
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), request.getGroupId(), GroupMemberStatus.INVITED))
                    .thenReturn(false);
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), request.getGroupId(), GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(false);
            
            // Mock validation service to throw exception
            doThrow(new GroupException("User has reached the maximum number of groups (10). Cannot join more groups."))
                    .when(groupValidationService).validateUserCanJoinGroup(targetUser.getUserId(), request.getGroupId());

            // When & Then
            GroupException exception = assertThrows(GroupException.class, () ->
                groupInvitationService.sendEmailInvitation(request)
            );
            
            assertEquals("User has reached the maximum number of groups (10). Cannot join more groups.", 
                    exception.getMessage());
            
            verify(groupValidationService).validateUserCanJoinGroup(targetUser.getUserId(), request.getGroupId());
            verify(groupMemberRepository, never()).save(any());
        }
    }

    @Test
    void sendEmailInvitation_WhenUserIsAlreadyMember_ShouldThrowException() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(request.getGroupId())).thenReturn(Optional.of(testGroup));
            when(userRepository.findByUserEmailAndUserIsActiveTrue(request.getEmail()))
                    .thenReturn(Optional.of(targetUser));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), request.getGroupId()))
                    .thenReturn(Optional.of(createOwnerGroupMember()));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.OWNER))
                    .thenReturn(true);
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), request.getGroupId(), GroupMemberStatus.ACTIVE))
                    .thenReturn(true); // User is already a member

            // When & Then
            GroupException exception = assertThrows(GroupException.class, () ->
                groupInvitationService.sendEmailInvitation(request)
            );
            
            assertEquals("User is already a member of this group", exception.getMessage());
            
            verify(groupValidationService, never()).validateUserCanJoinGroup(any(), any());
            verify(groupMemberRepository, never()).save(any());
        }
    }

    private GroupMember createOwnerGroupMember() {
        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroupMemberId(1);
        ownerMember.setUser(currentUser);
        ownerMember.setGroup(testGroup);
        ownerMember.setRole(GroupMemberRole.OWNER);
        ownerMember.setStatus(GroupMemberStatus.ACTIVE);
        ownerMember.setJoinDate(LocalDateTime.now().minusDays(30));
        return ownerMember;
    }

    // ================= PROCESS INVITATION TOKEN TESTS =================

    @Test
    void processInvitationToken_WhenGroupIsFull_ShouldReturnGroupFullResponse() {
        // Given
        String invitationToken = "valid-token";
        InvitationTokenData tokenData = InvitationTokenData.builder()
                .groupId(1)
                .invitedUserId(2)
                .status("INVITED")
                .build();
        
        GroupMember invitation = new GroupMember();
        invitation.setUser(targetUser);
        invitation.setGroup(testGroup);
        invitation.setStatus(GroupMemberStatus.INVITED);
        
        when(invitationTokenService.getInvitationTokenData(invitationToken))
                .thenReturn(Optional.of(tokenData));
        when(groupRepository.findById(tokenData.getGroupId()))
                .thenReturn(Optional.of(testGroup));
        when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                tokenData.getInvitedUserId(), tokenData.getGroupId()))
                .thenReturn(Optional.of(invitation));
        
        // Mock validation service to throw exception for group full
        doThrow(new GroupException("Group has reached the maximum number of members (20). Cannot accept more members."))
                .when(groupValidationService).validateGroupCanAcceptMoreMembers(tokenData.getGroupId());

        // When
        GroupInvitationLandingResponse response = groupInvitationService.processInvitationToken(invitationToken);

        // Then
        assertNotNull(response);
        assertEquals(ResultType.GROUP_FULL, response.getResultType());
        assertEquals("Group has reached the maximum number of members (20). Cannot accept more members.", 
                response.getMessage());
        
        verify(groupValidationService).validateGroupCanAcceptMoreMembers(tokenData.getGroupId());
        verify(groupMemberRepository, never()).save(any());
        verify(invitationTokenService, never()).updateInvitationTokenStatus(any(), any());
    }
}
