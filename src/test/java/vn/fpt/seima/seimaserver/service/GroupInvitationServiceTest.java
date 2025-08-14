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

    
    // ================= SEND EMAIL INVITATION TESTS =================

    @Test
    void sendEmailInvitation_WhenValidRequest_ShouldSendInvitationSuccessfully() {
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
            when(appProperties.getLabName()).thenReturn("Test App");
            
            // When
            EmailInvitationResponse response = groupInvitationService.sendEmailInvitation(request);
            
            // Then
            assertNotNull(response);
            assertEquals(request.getGroupId(), response.getGroupId());
            assertEquals(testGroup.getGroupName(), response.getGroupName());
            assertEquals(request.getEmail(), response.getInvitedEmail());
            assertTrue(response.isUserExists());
            assertTrue(response.isEmailSent());
            assertNotNull(response.getInviteLink());
            assertNotNull(response.getMessage());
            
            verify(groupValidationService).validateUserCanJoinGroup(targetUser.getUserId(), request.getGroupId());
            verify(groupMemberRepository).save(any(GroupMember.class));
            verify(invitationTokenService).createInvitationToken(any());
            verify(emailService).sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any());
        }
    }

    @Test
    void sendEmailInvitation_WhenRequestIsNull_ShouldThrowException() {
        // Given
        EmailInvitationRequest nullRequest = null;
        
        // When & Then
        GroupException exception = assertThrows(GroupException.class, () ->
            groupInvitationService.sendEmailInvitation(nullRequest)
        );
        
        assertEquals("Email invitation request cannot be null", exception.getMessage());
        
        verify(groupRepository, never()).findById(any());
        verify(userRepository, never()).findByUserEmailAndUserIsActiveTrue(any());
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void sendEmailInvitation_WhenGroupIdIsNull_ShouldThrowException() {
        // Given
        EmailInvitationRequest invalidRequest = EmailInvitationRequest.builder()
                .groupId(null)
                .email("test@example.com")
                .build();
        
        // When & Then
        GroupException exception = assertThrows(GroupException.class, () ->
            groupInvitationService.sendEmailInvitation(invalidRequest)
        );
        
        assertEquals("Group ID is required", exception.getMessage());
        
        verify(groupRepository, never()).findById(any());
        verify(userRepository, never()).findByUserEmailAndUserIsActiveTrue(any());
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void sendEmailInvitation_WhenEmailIsNull_ShouldThrowException() {
        // Given
        EmailInvitationRequest invalidRequest = EmailInvitationRequest.builder()
                .groupId(1)
                .email(null)
                .build();
        
        // When & Then
        GroupException exception = assertThrows(GroupException.class, () ->
            groupInvitationService.sendEmailInvitation(invalidRequest)
        );
        
        assertEquals("Email is required", exception.getMessage());
        
        verify(groupRepository, never()).findById(any());
        verify(userRepository, never()).findByUserEmailAndUserIsActiveTrue(any());
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void sendEmailInvitation_WhenEmailIsEmpty_ShouldThrowException() {
        // Given
        EmailInvitationRequest invalidRequest = EmailInvitationRequest.builder()
                .groupId(1)
                .email("")
                .build();
        
        // When & Then
        GroupException exception = assertThrows(GroupException.class, () ->
            groupInvitationService.sendEmailInvitation(invalidRequest)
        );
        
        assertEquals("Email is required", exception.getMessage());
        
        verify(groupRepository, never()).findById(any());
        verify(userRepository, never()).findByUserEmailAndUserIsActiveTrue(any());
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void sendEmailInvitation_WhenEmailIsBlank_ShouldThrowException() {
        // Given
        EmailInvitationRequest invalidRequest = EmailInvitationRequest.builder()
                .groupId(1)
                .email("   ")
                .build();
        
        // When & Then
        GroupException exception = assertThrows(GroupException.class, () ->
            groupInvitationService.sendEmailInvitation(invalidRequest)
        );
        
        assertEquals("Email is required", exception.getMessage());
        
        verify(groupRepository, never()).findById(any());
        verify(userRepository, never()).findByUserEmailAndUserIsActiveTrue(any());
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void sendEmailInvitation_WhenEmailFormatIsInvalid_ShouldThrowException() {
        // Given
        EmailInvitationRequest invalidRequest = EmailInvitationRequest.builder()
                .groupId(1)
                .email("invalid-email")
                .build();
        
        // When & Then
        GroupException exception = assertThrows(GroupException.class, () ->
            groupInvitationService.sendEmailInvitation(invalidRequest)
        );
        
        assertEquals("Invalid email format", exception.getMessage());
        
        verify(groupRepository, never()).findById(any());
        verify(userRepository, never()).findByUserEmailAndUserIsActiveTrue(any());
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void sendEmailInvitation_WhenEmailFormatIsInvalidWithAtSymbol_ShouldThrowException() {
        // Given
        EmailInvitationRequest invalidRequest = EmailInvitationRequest.builder()
                .groupId(1)
                .email("test@")
                .build();
        
        // When & Then
        GroupException exception = assertThrows(GroupException.class, () ->
            groupInvitationService.sendEmailInvitation(invalidRequest)
        );
        
        assertEquals("Invalid email format", exception.getMessage());
        
        verify(groupRepository, never()).findById(any());
        verify(userRepository, never()).findByUserEmailAndUserIsActiveTrue(any());
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void sendEmailInvitation_WhenGroupNotFound_ShouldThrowException() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(request.getGroupId())).thenReturn(Optional.empty());
            
            // When & Then
            GroupException exception = assertThrows(GroupException.class, () ->
                groupInvitationService.sendEmailInvitation(request)
            );
            
            assertEquals("Group not found", exception.getMessage());
            
            verify(groupRepository).findById(request.getGroupId());
            verify(userRepository, never()).findByUserEmailAndUserIsActiveTrue(any());
            verify(groupMemberRepository, never()).save(any());
        }
    }

    @Test
    void sendEmailInvitation_WhenGroupIsInactive_ShouldThrowException() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            Group inactiveGroup = new Group();
            inactiveGroup.setGroupId(request.getGroupId());
            inactiveGroup.setGroupName("Inactive Group");
            inactiveGroup.setGroupIsActive(false);
            
            when(groupRepository.findById(request.getGroupId())).thenReturn(Optional.of(inactiveGroup));
            
            // When & Then
            GroupException exception = assertThrows(GroupException.class, () ->
                groupInvitationService.sendEmailInvitation(request)
            );
            
            assertEquals("Group not found", exception.getMessage());
            
            verify(groupRepository).findById(request.getGroupId());
            verify(userRepository, never()).findByUserEmailAndUserIsActiveTrue(any());
            verify(groupMemberRepository, never()).save(any());
        }
    }

    @Test
    void sendEmailInvitation_WhenCurrentUserNotMember_ShouldThrowException() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(request.getGroupId())).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), request.getGroupId()))
                    .thenReturn(Optional.empty());
            
            // When & Then
            GroupException exception = assertThrows(GroupException.class, () ->
                groupInvitationService.sendEmailInvitation(request)
            );
            
            assertEquals("You are not an active member of this group", exception.getMessage());
            
            verify(groupRepository).findById(request.getGroupId());
            verify(groupMemberRepository).findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), request.getGroupId());
            verify(userRepository, never()).findByUserEmailAndUserIsActiveTrue(any());
            verify(groupMemberRepository, never()).save(any());
        }
    }

    @Test
    void sendEmailInvitation_WhenCurrentUserInactiveMember_ShouldThrowException() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            GroupMember inactiveMember = new GroupMember();
            inactiveMember.setUser(currentUser);
            inactiveMember.setGroup(testGroup);
            inactiveMember.setStatus(GroupMemberStatus.LEFT);
            
            when(groupRepository.findById(request.getGroupId())).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), request.getGroupId()))
                    .thenReturn(Optional.of(inactiveMember));
            
            // When & Then
            GroupException exception = assertThrows(GroupException.class, () ->
                groupInvitationService.sendEmailInvitation(request)
            );
            
            assertEquals("You are not an active member of this group", exception.getMessage());
            
            verify(groupRepository).findById(request.getGroupId());
            verify(groupMemberRepository).findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), request.getGroupId());
            verify(userRepository, never()).findByUserEmailAndUserIsActiveTrue(any());
            verify(groupMemberRepository, never()).save(any());
        }
    }

    @Test
    void sendEmailInvitation_WhenCurrentUserNoPermission_ShouldThrowException() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            GroupMember member = new GroupMember();
            member.setUser(currentUser);
            member.setGroup(testGroup);
            member.setStatus(GroupMemberStatus.ACTIVE);
            member.setRole(GroupMemberRole.MEMBER);
            
            when(groupRepository.findById(request.getGroupId())).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), request.getGroupId()))
                    .thenReturn(Optional.of(member));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.MEMBER))
                    .thenReturn(false);
            
            // When & Then
            GroupException exception = assertThrows(GroupException.class, () ->
                groupInvitationService.sendEmailInvitation(request)
            );
            
            assertEquals("You don't have permission to invite members to this group", exception.getMessage());
            
            verify(groupRepository).findById(request.getGroupId());
            verify(groupMemberRepository).findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), request.getGroupId());
            verify(groupPermissionService).canInviteMembers(GroupMemberRole.MEMBER);
            verify(userRepository, never()).findByUserEmailAndUserIsActiveTrue(any());
            verify(groupMemberRepository, never()).save(any());
        }
    }

    @Test
    void sendEmailInvitation_WhenTargetUserNotFound_ShouldReturnUserNotExistsResponse() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(request.getGroupId())).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), request.getGroupId()))
                    .thenReturn(Optional.of(createOwnerGroupMember()));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.OWNER))
                    .thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue(request.getEmail()))
                    .thenReturn(Optional.empty());
            
            // When
            EmailInvitationResponse response = groupInvitationService.sendEmailInvitation(request);
            
            // Then
            assertNotNull(response);
            assertEquals(request.getGroupId(), response.getGroupId());
            assertEquals(testGroup.getGroupName(), response.getGroupName());
            assertEquals(request.getEmail(), response.getInvitedEmail());
            assertFalse(response.isUserExists());
            assertFalse(response.isEmailSent());
            assertNull(response.getInviteLink());
            
            verify(groupRepository).findById(request.getGroupId());
            verify(groupMemberRepository).findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), request.getGroupId());
            verify(groupPermissionService).canInviteMembers(GroupMemberRole.OWNER);
            verify(userRepository).findByUserEmailAndUserIsActiveTrue(request.getEmail());
            verify(groupMemberRepository, never()).save(any());
            verify(invitationTokenService, never()).createInvitationToken(any());
            verify(emailService, never()).sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any());
        }
    }

    @Test
    void sendEmailInvitation_WhenTargetUserAlreadyActiveMember_ShouldThrowException() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            when(groupRepository.findById(request.getGroupId())).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), request.getGroupId()))
                    .thenReturn(Optional.of(createOwnerGroupMember()));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.OWNER))
                    .thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue(request.getEmail()))
                    .thenReturn(Optional.of(targetUser));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), request.getGroupId(), GroupMemberStatus.ACTIVE))
                    .thenReturn(true);
            
            // When & Then
            GroupException exception = assertThrows(GroupException.class, () ->
                groupInvitationService.sendEmailInvitation(request)
            );
            
            assertEquals("User is already a member of this group", exception.getMessage());
            
            verify(groupRepository).findById(request.getGroupId());
            verify(groupMemberRepository).findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), request.getGroupId());
            verify(groupPermissionService).canInviteMembers(GroupMemberRole.OWNER);
            verify(userRepository).findByUserEmailAndUserIsActiveTrue(request.getEmail());
            verify(groupMemberRepository).existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), request.getGroupId(), GroupMemberStatus.ACTIVE);
            verify(groupMemberRepository, never()).save(any());
            verify(invitationTokenService, never()).createInvitationToken(any());
            verify(emailService, never()).sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any());
        }
    }

    @Test
    void sendEmailInvitation_WhenTargetUserAlreadyInvited_ShouldThrowException() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            GroupMember existingInvitation = new GroupMember();
            existingInvitation.setUser(targetUser);
            existingInvitation.setGroup(testGroup);
            existingInvitation.setStatus(GroupMemberStatus.INVITED);
            
            when(groupRepository.findById(request.getGroupId())).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), request.getGroupId()))
                    .thenReturn(Optional.of(createOwnerGroupMember()));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.OWNER))
                    .thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue(request.getEmail()))
                    .thenReturn(Optional.of(targetUser));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), request.getGroupId(), GroupMemberStatus.ACTIVE))
                    .thenReturn(false);
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                    targetUser.getUserId(), request.getGroupId()))
                    .thenReturn(Optional.of(existingInvitation));
            
            // When & Then
            GroupException exception = assertThrows(GroupException.class, () ->
                groupInvitationService.sendEmailInvitation(request)
            );
            
            assertEquals("User has already been invited to this group", exception.getMessage());
            
            verify(groupRepository).findById(request.getGroupId());
            verify(groupMemberRepository).findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), request.getGroupId());
            verify(groupPermissionService).canInviteMembers(GroupMemberRole.OWNER);
            verify(userRepository).findByUserEmailAndUserIsActiveTrue(request.getEmail());
            verify(groupMemberRepository).existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), request.getGroupId(), GroupMemberStatus.ACTIVE);
            verify(groupMemberRepository).findMostRecentMembershipByUserIdAndGroupId(
                    targetUser.getUserId(), request.getGroupId());
            verify(groupMemberRepository, never()).save(any());
            verify(invitationTokenService, never()).createInvitationToken(any());
            verify(emailService, never()).sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any());
        }
    }

    @Test
    void sendEmailInvitation_WhenTargetUserHasPendingApproval_ShouldThrowException() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            GroupMember pendingApproval = new GroupMember();
            pendingApproval.setUser(targetUser);
            pendingApproval.setGroup(testGroup);
            pendingApproval.setStatus(GroupMemberStatus.PENDING_APPROVAL);
            
            when(groupRepository.findById(request.getGroupId())).thenReturn(Optional.of(testGroup));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), request.getGroupId()))
                    .thenReturn(Optional.of(createOwnerGroupMember()));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.OWNER))
                    .thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue(request.getEmail()))
                    .thenReturn(Optional.of(targetUser));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), request.getGroupId(), GroupMemberStatus.ACTIVE))
                    .thenReturn(false);
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                    targetUser.getUserId(), request.getGroupId()))
                    .thenReturn(Optional.of(pendingApproval));
            
            // When & Then
            GroupException exception = assertThrows(GroupException.class, () ->
                groupInvitationService.sendEmailInvitation(request)
            );
            
            assertEquals("User already has a pending invitation to this group", exception.getMessage());
            
            verify(groupRepository).findById(request.getGroupId());
            verify(groupMemberRepository).findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), request.getGroupId());
            verify(groupPermissionService).canInviteMembers(GroupMemberRole.OWNER);
            verify(userRepository).findByUserEmailAndUserIsActiveTrue(request.getEmail());
            verify(groupMemberRepository).existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), request.getGroupId(), GroupMemberStatus.ACTIVE);
            verify(groupMemberRepository).findMostRecentMembershipByUserIdAndGroupId(
                    targetUser.getUserId(), request.getGroupId());
            verify(groupMemberRepository, never()).save(any());
            verify(invitationTokenService, never()).createInvitationToken(any());
            verify(emailService, never()).sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any());
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
            verify(invitationTokenService, never()).createInvitationToken(any());
            verify(emailService, never()).sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any());
        }
    }

    @Test
    void sendEmailInvitation_WhenEmailServiceFails_ShouldReturnResponseWithEmailNotSent() {
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
            
            // Mock invitation token service
            when(invitationTokenService.createInvitationToken(any())).thenReturn("test-token");
            
            // Mock app properties
            AppProperties.Client mockClient = new AppProperties.Client();
            mockClient.setBaseUrl("http://example.com");
            when(appProperties.getClient()).thenReturn(mockClient);
            when(appProperties.getLabName()).thenReturn("Test App");
            
            // Mock email service to throw exception
            doThrow(new RuntimeException("Email service unavailable"))
                    .when(emailService).sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any());
            
            // When
            EmailInvitationResponse response = groupInvitationService.sendEmailInvitation(request);
            
            // Then
            assertNotNull(response);
            assertEquals(request.getGroupId(), response.getGroupId());
            assertEquals(testGroup.getGroupName(), response.getGroupName());
            assertEquals(request.getEmail(), response.getInvitedEmail());
            assertTrue(response.isUserExists());
            assertFalse(response.isEmailSent());
            assertNotNull(response.getInviteLink());
            assertNotNull(response.getMessage());
            
            verify(groupValidationService).validateUserCanJoinGroup(targetUser.getUserId(), request.getGroupId());
            verify(groupMemberRepository).save(any(GroupMember.class));
            verify(invitationTokenService).createInvitationToken(any());
            verify(emailService).sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any());
        }
    }

    @Test
    void sendEmailInvitation_WhenBoundaryValues_ShouldHandleCorrectly() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given - Test with minimum valid values
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            EmailInvitationRequest boundaryRequest = EmailInvitationRequest.builder()
                    .groupId(1)
                    .email("a@b.co")
                    .build();
            
            when(groupRepository.findById(boundaryRequest.getGroupId())).thenReturn(Optional.of(testGroup));
            when(userRepository.findByUserEmailAndUserIsActiveTrue(boundaryRequest.getEmail()))
                    .thenReturn(Optional.of(targetUser));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), boundaryRequest.getGroupId()))
                    .thenReturn(Optional.of(createOwnerGroupMember()));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.OWNER))
                    .thenReturn(true);
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), boundaryRequest.getGroupId(), GroupMemberStatus.ACTIVE))
                    .thenReturn(false);
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), boundaryRequest.getGroupId(), GroupMemberStatus.INVITED))
                    .thenReturn(false);
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), boundaryRequest.getGroupId(), GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(false);
            
            doNothing().when(groupValidationService).validateUserCanJoinGroup(targetUser.getUserId(), boundaryRequest.getGroupId());
            doNothing().when(emailService).sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any());
            when(invitationTokenService.createInvitationToken(any())).thenReturn("test-token");
            
            // Mock app properties
            AppProperties.Client mockClient = new AppProperties.Client();
            mockClient.setBaseUrl("http://example.com");
            when(appProperties.getClient()).thenReturn(mockClient);
            when(appProperties.getLabName()).thenReturn("Test App");
            
            // When
            EmailInvitationResponse response = groupInvitationService.sendEmailInvitation(boundaryRequest);
            
            // Then
            assertNotNull(response);
            assertEquals(boundaryRequest.getGroupId(), response.getGroupId());
            assertEquals(testGroup.getGroupName(), response.getGroupName());
            assertEquals(boundaryRequest.getEmail(), response.getInvitedEmail());
            assertTrue(response.isUserExists());
            assertTrue(response.isEmailSent());
        }
    }

    @Test
    void sendEmailInvitation_WhenLargeValues_ShouldHandleCorrectly() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class)) {
            // Given - Test with large values
            mockedUserUtils.when(UserUtils::getCurrentUser).thenReturn(currentUser);
            
            EmailInvitationRequest largeRequest = EmailInvitationRequest.builder()
                    .groupId(999999999)
                    .email("verylongemailaddress@verylongdomainname.verylongtld")
                    .build();
            
            Group largeGroup = new Group();
            largeGroup.setGroupId(largeRequest.getGroupId());
            largeGroup.setGroupName("Very Long Group Name With Many Characters To Test Boundary Conditions");
            largeGroup.setGroupIsActive(true);
            
            when(groupRepository.findById(largeRequest.getGroupId())).thenReturn(Optional.of(largeGroup));
            when(userRepository.findByUserEmailAndUserIsActiveTrue(largeRequest.getEmail()))
                    .thenReturn(Optional.of(targetUser));
            when(groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                    currentUser.getUserId(), largeRequest.getGroupId()))
                    .thenReturn(Optional.of(createOwnerGroupMember()));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.OWNER))
                    .thenReturn(true);
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), largeRequest.getGroupId(), GroupMemberStatus.ACTIVE))
                    .thenReturn(false);
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), largeRequest.getGroupId(), GroupMemberStatus.INVITED))
                    .thenReturn(false);
            when(groupMemberRepository.existsByUserAndGroupAndStatus(
                    targetUser.getUserId(), largeRequest.getGroupId(), GroupMemberStatus.PENDING_APPROVAL))
                    .thenReturn(false);
            
            doNothing().when(groupValidationService).validateUserCanJoinGroup(targetUser.getUserId(), largeRequest.getGroupId());
            doNothing().when(emailService).sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any());
            when(invitationTokenService.createInvitationToken(any())).thenReturn("test-token");
            
            // Mock app properties
            AppProperties.Client mockClient = new AppProperties.Client();
            mockClient.setBaseUrl("http://example.com");
            when(appProperties.getClient()).thenReturn(mockClient);
            when(appProperties.getLabName()).thenReturn("Test App");
            
            // When
            EmailInvitationResponse response = groupInvitationService.sendEmailInvitation(largeRequest);
            
            // Then
            assertNotNull(response);
            assertEquals(largeRequest.getGroupId(), response.getGroupId());
            assertEquals(largeGroup.getGroupName(), response.getGroupName());
            assertEquals(largeRequest.getEmail(), response.getInvitedEmail());
            assertTrue(response.isUserExists());
            assertTrue(response.isEmailSent());
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

    
        // ================= HANDLE SUCCESS ACCEPTANCE TESTS =================

        @Test
        void handleSuccessAcceptance_WhenValidParameters_ShouldReturnSuccessResponse() {
            // Given
            Long userId = 1L;
            Long groupId = 1L;

            User user = User.builder()
                    .userId(userId.intValue())
                    .userEmail("user@example.com")
                    .userFullName("Test User")
                    .userIsActive(true)
                    .build();

            Group group = new Group();
            group.setGroupId(groupId.intValue());
            group.setGroupName("Test Group");
            group.setGroupIsActive(true);

            GroupMember groupMember = new GroupMember();
            groupMember.setGroupMemberId(1);
            groupMember.setUser(user);
            groupMember.setGroup(group);
            groupMember.setStatus(GroupMemberStatus.ACTIVE);

            BranchLinkResponse branchLinkResponse = BranchLinkResponse.builder()
                    .url("https://example.com/group/1")
                    .build();

            when(userRepository.findById(userId.intValue())).thenReturn(Optional.of(user));
            when(groupRepository.findById(groupId.intValue())).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    userId.intValue(), groupId.intValue(), GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(groupMember));
            when(branchLinkService.createInvitationDeepLink(
                    groupId.intValue(), userId.intValue(), null, "VIEW_GROUP"))
                    .thenReturn(branchLinkResponse);

            // When
            SuccessAcceptanceGroupResponse response = groupInvitationService.handleSuccessAcceptance(userId, groupId);

            // Then
            assertNotNull(response);
            assertEquals(ResultType.SUCCESS, response.getResultType());
            assertEquals("https://example.com/group/1", response.getJoinButtonLink());
            assertEquals("Successfully accepted invitation to group", response.getMessage());

            verify(userRepository).findById(userId.intValue());
            verify(groupRepository).findById(groupId.intValue());
            verify(groupMemberRepository).findByUserAndGroupAndStatus(
                    userId.intValue(), groupId.intValue(), GroupMemberStatus.ACTIVE);
            verify(branchLinkService).createInvitationDeepLink(
                    groupId.intValue(), userId.intValue(), null, "VIEW_GROUP");
        }

        @Test
        void handleSuccessAcceptance_WhenUserIdIsNull_ShouldReturnInvalidTokenResponse() {
            // Given
            Long userId = null;
            Long groupId = 1L;

            // When
            SuccessAcceptanceGroupResponse response = groupInvitationService.handleSuccessAcceptance(userId, groupId);

            // Then
            assertNotNull(response);
            assertEquals(ResultType.INVALID_OR_USED_TOKEN, response.getResultType());
            assertEquals("Invalid user ID or group ID", response.getMessage());
            assertNull(response.getJoinButtonLink());

            verify(userRepository, never()).findById(any());
            verify(groupRepository, never()).findById(any());
            verify(groupMemberRepository, never()).findByUserAndGroupAndStatus(any(), any(), any());
            verify(branchLinkService, never()).createInvitationDeepLink(any(), any(), any(), any());
        }

        @Test
        void handleSuccessAcceptance_WhenGroupIdIsNull_ShouldReturnInvalidTokenResponse() {
            // Given
            Long userId = 1L;
            Long groupId = null;

            // When
            SuccessAcceptanceGroupResponse response = groupInvitationService.handleSuccessAcceptance(userId, groupId);

            // Then
            assertNotNull(response);
            assertEquals(ResultType.INVALID_OR_USED_TOKEN, response.getResultType());
            assertEquals("Invalid user ID or group ID", response.getMessage());
            assertNull(response.getJoinButtonLink());

            verify(userRepository, never()).findById(any());
            verify(groupRepository, never()).findById(any());
            verify(groupMemberRepository, never()).findByUserAndGroupAndStatus(any(), any(), any());
            verify(branchLinkService, never()).createInvitationDeepLink(any(), any(), any(), any());
        }

        @Test
        void handleSuccessAcceptance_WhenBothParametersAreNull_ShouldReturnInvalidTokenResponse() {
            // Given
            Long userId = null;
            Long groupId = null;

            // When
            SuccessAcceptanceGroupResponse response = groupInvitationService.handleSuccessAcceptance(userId, groupId);

            // Then
            assertNotNull(response);
            assertEquals(ResultType.INVALID_OR_USED_TOKEN, response.getResultType());
            assertEquals("Invalid user ID or group ID", response.getMessage());
            assertNull(response.getJoinButtonLink());

            verify(userRepository, never()).findById(any());
            verify(groupRepository, never()).findById(any());
            verify(groupMemberRepository, never()).findByUserAndGroupAndStatus(any(), any(), any());
            verify(branchLinkService, never()).createInvitationDeepLink(any(), any(), any(), any());
        }

        @Test
        void handleSuccessAcceptance_WhenGroupNotFound_ShouldReturnGroupInactiveResponse() {
            // Given
            Long userId = 1L;
            Long groupId = 999L;

            when(groupRepository.findById(groupId.intValue())).thenReturn(Optional.empty());

            // When
            SuccessAcceptanceGroupResponse response = groupInvitationService.handleSuccessAcceptance(userId, groupId);

            // Then
            assertNotNull(response);
            assertEquals(ResultType.GROUP_INACTIVE_OR_DELETED, response.getResultType());
            assertEquals("Group not found", response.getMessage());
            assertNull(response.getJoinButtonLink());

            verify(groupRepository).findById(groupId.intValue());
            verify(userRepository, never()).findById(any());
            verify(groupMemberRepository, never()).findByUserAndGroupAndStatus(any(), any(), any());
            verify(branchLinkService, never()).createInvitationDeepLink(any(), any(), any(), any());
        }

        @Test
        void handleSuccessAcceptance_WhenGroupIsInactive_ShouldReturnGroupInactiveResponse() {
            // Given
            Long userId = 1L;
            Long groupId = 1L;

            Group inactiveGroup = new Group();
            inactiveGroup.setGroupId(groupId.intValue());
            inactiveGroup.setGroupName("Inactive Group");
            inactiveGroup.setGroupIsActive(false);

            when(groupRepository.findById(groupId.intValue())).thenReturn(Optional.of(inactiveGroup));

            // When
            SuccessAcceptanceGroupResponse response = groupInvitationService.handleSuccessAcceptance(userId, groupId);

            // Then
            assertNotNull(response);
            assertEquals(ResultType.GROUP_INACTIVE_OR_DELETED, response.getResultType());
            assertEquals("Group is no longer active", response.getMessage());
            assertNull(response.getJoinButtonLink());

            verify(groupRepository).findById(groupId.intValue());
            verify(userRepository, never()).findById(any());
            verify(groupMemberRepository, never()).findByUserAndGroupAndStatus(any(), any(), any());
            verify(branchLinkService, never()).createInvitationDeepLink(any(), any(), any(), any());
        }

        @Test
        void handleSuccessAcceptance_WhenUserNotFound_ShouldReturnInvalidTokenResponse() {
            // Given
            Long userId = 999L;
            Long groupId = 1L;

            Group group = new Group();
            group.setGroupId(groupId.intValue());
            group.setGroupName("Test Group");
            group.setGroupIsActive(true);

            when(groupRepository.findById(groupId.intValue())).thenReturn(Optional.of(group));
            when(userRepository.findById(userId.intValue())).thenReturn(Optional.empty());

            // When
            SuccessAcceptanceGroupResponse response = groupInvitationService.handleSuccessAcceptance(userId, groupId);

            // Then
            assertNotNull(response);
            assertEquals(ResultType.INVALID_OR_USED_TOKEN, response.getResultType());
            assertEquals("User not found", response.getMessage());
            assertNull(response.getJoinButtonLink());

            verify(groupRepository).findById(groupId.intValue());
            verify(userRepository).findById(userId.intValue());
            verify(groupMemberRepository, never()).findByUserAndGroupAndStatus(any(), any(), any());
            verify(branchLinkService, never()).createInvitationDeepLink(any(), any(), any(), any());
        }

        @Test
        void handleSuccessAcceptance_WhenUserIsInactive_ShouldReturnInvalidTokenResponse() {
            // Given
            Long userId = 1L;
            Long groupId = 1L;

            Group group = new Group();
            group.setGroupId(groupId.intValue());
            group.setGroupName("Test Group");
            group.setGroupIsActive(true);

            User inactiveUser = User.builder()
                    .userId(userId.intValue())
                    .userEmail("user@example.com")
                    .userFullName("Inactive User")
                    .userIsActive(false)
                    .build();

            when(groupRepository.findById(groupId.intValue())).thenReturn(Optional.of(group));
            when(userRepository.findById(userId.intValue())).thenReturn(Optional.of(inactiveUser));

            // When
            SuccessAcceptanceGroupResponse response = groupInvitationService.handleSuccessAcceptance(userId, groupId);

            // Then
            assertNotNull(response);
            assertEquals(ResultType.INVALID_OR_USED_TOKEN, response.getResultType());
            assertEquals("User account is not active", response.getMessage());
            assertNull(response.getJoinButtonLink());

            verify(groupRepository).findById(groupId.intValue());
            verify(userRepository).findById(userId.intValue());
            verify(groupMemberRepository, never()).findByUserAndGroupAndStatus(any(), any(), any());
            verify(branchLinkService, never()).createInvitationDeepLink(any(), any(), any(), any());
        }

        @Test
        void handleSuccessAcceptance_WhenUserIsNotActiveMember_ShouldReturnInvalidTokenResponse() {
            // Given
            Long userId = 1L;
            Long groupId = 1L;

            Group group = new Group();
            group.setGroupId(groupId.intValue());
            group.setGroupName("Test Group");
            group.setGroupIsActive(true);

            User user = User.builder()
                    .userId(userId.intValue())
                    .userEmail("user@example.com")
                    .userFullName("Test User")
                    .userIsActive(true)
                    .build();

            when(groupRepository.findById(groupId.intValue())).thenReturn(Optional.of(group));
            when(userRepository.findById(userId.intValue())).thenReturn(Optional.of(user));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    userId.intValue(), groupId.intValue(), GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            // When
            SuccessAcceptanceGroupResponse response = groupInvitationService.handleSuccessAcceptance(userId, groupId);

            // Then
            assertNotNull(response);
            assertEquals(ResultType.INVALID_OR_USED_TOKEN, response.getResultType());
            assertEquals("User is not a member of this group", response.getMessage());
            assertNull(response.getJoinButtonLink());

            verify(groupRepository).findById(groupId.intValue());
            verify(userRepository).findById(userId.intValue());
            verify(groupMemberRepository).findByUserAndGroupAndStatus(
                    userId.intValue(), groupId.intValue(), GroupMemberStatus.ACTIVE);
            verify(branchLinkService, never()).createInvitationDeepLink(any(), any(), any(), any());
        }

        @Test
        void handleSuccessAcceptance_WhenBranchLinkServiceFails_ShouldReturnInvalidTokenResponse() {
            // Given
            Long userId = 1L;
            Long groupId = 1L;

            User user = User.builder()
                    .userId(userId.intValue())
                    .userEmail("user@example.com")
                    .userFullName("Test User")
                    .userIsActive(true)
                    .build();

            Group group = new Group();
            group.setGroupId(groupId.intValue());
            group.setGroupName("Test Group");
            group.setGroupIsActive(true);

            GroupMember groupMember = new GroupMember();
            groupMember.setGroupMemberId(1);
            groupMember.setUser(user);
            groupMember.setGroup(group);
            groupMember.setStatus(GroupMemberStatus.ACTIVE);

            when(groupRepository.findById(groupId.intValue())).thenReturn(Optional.of(group));
            when(userRepository.findById(userId.intValue())).thenReturn(Optional.of(user));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    userId.intValue(), groupId.intValue(), GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(groupMember));
            when(branchLinkService.createInvitationDeepLink(
                    groupId.intValue(), userId.intValue(), null, "VIEW_GROUP"))
                    .thenThrow(new RuntimeException("Branch link service error"));

            // When
            SuccessAcceptanceGroupResponse response = groupInvitationService.handleSuccessAcceptance(userId, groupId);

            // Then
            assertNotNull(response);
            assertEquals(ResultType.INVALID_OR_USED_TOKEN, response.getResultType());
            assertEquals("Failed to create group access link", response.getMessage());
            assertNull(response.getJoinButtonLink());

            verify(groupRepository).findById(groupId.intValue());
            verify(userRepository).findById(userId.intValue());
            verify(groupMemberRepository).findByUserAndGroupAndStatus(
                    userId.intValue(), groupId.intValue(), GroupMemberStatus.ACTIVE);
            verify(branchLinkService).createInvitationDeepLink(
                    groupId.intValue(), userId.intValue(), null, "VIEW_GROUP");
        }

        @Test
        void handleSuccessAcceptance_WhenUnexpectedExceptionOccurs_ShouldReturnInvalidTokenResponse() {
            // Given
            Long userId = 1L;
            Long groupId = 1L;

            when(groupRepository.findById(groupId.intValue())).thenThrow(new RuntimeException("Database connection error"));

            // When
            SuccessAcceptanceGroupResponse response = groupInvitationService.handleSuccessAcceptance(userId, groupId);

            // Then
            assertNotNull(response);
            assertEquals(ResultType.INVALID_OR_USED_TOKEN, response.getResultType());
            assertEquals("An error occurred while processing your request", response.getMessage());
            assertNull(response.getJoinButtonLink());

            verify(groupRepository).findById(groupId.intValue());
            verify(userRepository, never()).findById(any());
            verify(groupMemberRepository, never()).findByUserAndGroupAndStatus(any(), any(), any());
            verify(branchLinkService, never()).createInvitationDeepLink(any(), any(), any(), any());
        }

        @Test
        void handleSuccessAcceptance_WhenBoundaryValues_ShouldHandleCorrectly() {
            // Given - Test with minimum valid values
            Long userId = 1L;
            Long groupId = 1L;

            User user = User.builder()
                    .userId(userId.intValue())
                    .userEmail("user@example.com")
                    .userFullName("Test User")
                    .userIsActive(true)
                    .build();

            Group group = new Group();
            group.setGroupId(groupId.intValue());
            group.setGroupName("Test Group");
            group.setGroupIsActive(true);

            GroupMember groupMember = new GroupMember();
            groupMember.setGroupMemberId(1);
            groupMember.setUser(user);
            groupMember.setGroup(group);
            groupMember.setStatus(GroupMemberStatus.ACTIVE);

            BranchLinkResponse branchLinkResponse = BranchLinkResponse.builder()
                    .url("https://example.com/group/1")
                    .build();

            when(userRepository.findById(userId.intValue())).thenReturn(Optional.of(user));
            when(groupRepository.findById(groupId.intValue())).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    userId.intValue(), groupId.intValue(), GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(groupMember));
            when(branchLinkService.createInvitationDeepLink(
                    groupId.intValue(), userId.intValue(), null, "VIEW_GROUP"))
                    .thenReturn(branchLinkResponse);

            // When
            SuccessAcceptanceGroupResponse response = groupInvitationService.handleSuccessAcceptance(userId, groupId);

            // Then
            assertNotNull(response);
            assertEquals(ResultType.SUCCESS, response.getResultType());
            assertEquals("https://example.com/group/1", response.getJoinButtonLink());
            assertEquals("Successfully accepted invitation to group", response.getMessage());
        }

        @Test
        void handleSuccessAcceptance_WhenLargeValues_ShouldHandleCorrectly() {
            // Given - Test with large values
            Long userId = 999999999L;
            Long groupId = 999999999L;

            User user = User.builder()
                    .userId(userId.intValue())
                    .userEmail("user@example.com")
                    .userFullName("Test User")
                    .userIsActive(true)
                    .build();

            Group group = new Group();
            group.setGroupId(groupId.intValue());
            group.setGroupName("Test Group");
            group.setGroupIsActive(true);

            GroupMember groupMember = new GroupMember();
            groupMember.setGroupMemberId(1);
            groupMember.setUser(user);
            groupMember.setGroup(group);
            groupMember.setStatus(GroupMemberStatus.ACTIVE);

            BranchLinkResponse branchLinkResponse = BranchLinkResponse.builder()
                    .url("https://example.com/group/999999999")
                    .build();

            when(userRepository.findById(userId.intValue())).thenReturn(Optional.of(user));
            when(groupRepository.findById(groupId.intValue())).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserAndGroupAndStatus(
                    userId.intValue(), groupId.intValue(), GroupMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(groupMember));
            when(branchLinkService.createInvitationDeepLink(
                    groupId.intValue(), userId.intValue(), null, "VIEW_GROUP"))
                    .thenReturn(branchLinkResponse);

            // When
            SuccessAcceptanceGroupResponse response = groupInvitationService.handleSuccessAcceptance(userId, groupId);

            // Then
            assertNotNull(response);
            assertEquals(ResultType.SUCCESS, response.getResultType());
            assertEquals("https://example.com/group/999999999", response.getJoinButtonLink());
            assertEquals("Successfully accepted invitation to group", response.getMessage());
        }
}
