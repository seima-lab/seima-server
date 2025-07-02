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
import vn.fpt.seima.seimaserver.dto.response.group.EmailInvitationResponse;
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

@ExtendWith(MockitoExtension.class)
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
    private AppProperties appProperties;

    @InjectMocks
    private GroupInvitationServiceImpl groupInvitationService;

    private EmailInvitationRequest validRequest;
    private User mockCurrentUser;
    private User mockTargetUser;
    private Group mockGroup;
    private GroupMember mockCurrentUserMembership;

    @BeforeEach
    void setUp() {
        // Setup valid request
        validRequest = EmailInvitationRequest.builder()
                .groupId(1)
                .email("target@example.com")
                .build();

        // Setup current user (inviter)
        mockCurrentUser = User.builder()
                .userId(1)
                .userEmail("inviter@example.com")
                .userFullName("John Inviter")
                .userIsActive(true)
                .build();

        // Setup target user
        mockTargetUser = User.builder()
                .userId(2)
                .userEmail("target@example.com")
                .userFullName("Jane Target")
                .userIsActive(true)
                .build();

        // Setup group
        mockGroup = new Group();
        mockGroup.setGroupId(1);
        mockGroup.setGroupName("Test Group");
        mockGroup.setGroupInviteCode("test-invite-123");
        mockGroup.setGroupAvatarUrl("https://example.com/avatar.jpg");
        mockGroup.setGroupIsActive(true);
        mockGroup.setGroupCreatedDate(LocalDateTime.now());

        // Setup current user membership (admin role)
        mockCurrentUserMembership = new GroupMember();
        mockCurrentUserMembership.setGroupMemberId(1);
        mockCurrentUserMembership.setGroup(mockGroup);
        mockCurrentUserMembership.setUser(mockCurrentUser);
        mockCurrentUserMembership.setRole(GroupMemberRole.ADMIN);
        mockCurrentUserMembership.setStatus(GroupMemberStatus.ACTIVE);

        // Setup AppProperties mock
        setupAppPropertiesMock();
    }

    private void setupAppPropertiesMock() {
        AppProperties.Client clientConfig = new AppProperties.Client();
        clientConfig.setBaseUrl("https://seima.app.com");
        lenient().when(appProperties.getClient()).thenReturn(clientConfig);
        lenient().when(appProperties.getLabName()).thenReturn("Seima");
    }

    // ===== SEND EMAIL INVITATION TESTS =====

    @Test
    void sendEmailInvitation_Success_WhenAllConditionsMet() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            
            when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(1, 1))
                    .thenReturn(Optional.of(mockCurrentUserMembership));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.ADMIN)).thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue("target@example.com"))
                    .thenReturn(Optional.of(mockTargetUser));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(2, 1, GroupMemberStatus.ACTIVE))
                    .thenReturn(false);
            when(groupMemberRepository.findByUserIdAndGroupId(2, 1))
                    .thenReturn(Optional.empty());
            when(groupMemberRepository.countActiveGroupMembers(1, GroupMemberStatus.ACTIVE))
                    .thenReturn(5L);
            
            // Mock email service to succeed
            doNothing().when(emailService).sendEmailWithHtmlTemplate(
                    anyString(), anyString(), anyString(), any(Context.class));

            // When
            EmailInvitationResponse result = groupInvitationService.sendEmailInvitation(validRequest);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getGroupId());
            assertEquals("Test Group", result.getGroupName());
            assertEquals("target@example.com", result.getInvitedEmail());
            assertTrue(result.isUserExists());
            assertTrue(result.isEmailSent());
            assertEquals("Invitation sent successfully", result.getMessage());
            assertNotNull(result.getInviteLink());
            assertTrue(result.getInviteLink().contains("test-invite-123"));

            // Verify email was sent
            verify(emailService).sendEmailWithHtmlTemplate(
                    eq("target@example.com"), 
                    eq("Group invitation to 'Test Group' on Seima"),
                    eq("group-invitation"),
                    any(Context.class)
            );
        }
    }

    @Test
    void sendEmailInvitation_Success_WhenEmailServiceFails() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            
            when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(1, 1))
                    .thenReturn(Optional.of(mockCurrentUserMembership));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.ADMIN)).thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue("target@example.com"))
                    .thenReturn(Optional.of(mockTargetUser));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(2, 1, GroupMemberStatus.ACTIVE))
                    .thenReturn(false);
            when(groupMemberRepository.findByUserIdAndGroupId(2, 1))
                    .thenReturn(Optional.empty());
            when(groupMemberRepository.countActiveGroupMembers(1, GroupMemberStatus.ACTIVE))
                    .thenReturn(5L);
            
            // Mock email service to fail
            doThrow(new RuntimeException("Email service error"))
                    .when(emailService).sendEmailWithHtmlTemplate(
                            anyString(), anyString(), anyString(), any(Context.class));

            // When
            EmailInvitationResponse result = groupInvitationService.sendEmailInvitation(validRequest);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getGroupId());
            assertEquals("Test Group", result.getGroupName());
            assertEquals("target@example.com", result.getInvitedEmail());
            assertTrue(result.isUserExists());
            assertFalse(result.isEmailSent());
            assertEquals("Failed to send invitation email", result.getMessage());
        }
    }

    @Test
    void sendEmailInvitation_ReturnsUserNotExists_WhenTargetUserNotFound() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            
            when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(1, 1))
                    .thenReturn(Optional.of(mockCurrentUserMembership));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.ADMIN)).thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue("target@example.com"))
                    .thenReturn(Optional.empty());

            // When
            EmailInvitationResponse result = groupInvitationService.sendEmailInvitation(validRequest);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getGroupId());
            assertEquals("Test Group", result.getGroupName());
            assertEquals("target@example.com", result.getInvitedEmail());
            assertFalse(result.isUserExists());
            assertFalse(result.isEmailSent());
            assertNull(result.getInviteLink());
            assertEquals("User account does not exist. User needs to register an account before joining the group.", 
                        result.getMessage());

            // Verify email was not sent
            verify(emailService, never()).sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any());
        }
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenRequestIsNull() {
        // When & Then
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            groupInvitationService.sendEmailInvitation(null);
        });
        
        // Implementation tries to access request.getGroupId() for logging before validation
        assertNotNull(exception);
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenGroupIdIsNull() {
        // Given
        validRequest.setGroupId(null);

        // When & Then
        GroupException exception = assertThrows(GroupException.class, () -> {
            groupInvitationService.sendEmailInvitation(validRequest);
        });
        
        assertEquals("Group ID is required", exception.getMessage());
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenEmailIsNull() {
        // Given
        validRequest.setEmail(null);

        // When & Then
        GroupException exception = assertThrows(GroupException.class, () -> {
            groupInvitationService.sendEmailInvitation(validRequest);
        });
        
        assertEquals("Email is required", exception.getMessage());
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenEmailIsEmpty() {
        // Given
        validRequest.setEmail("");

        // When & Then
        GroupException exception = assertThrows(GroupException.class, () -> {
            groupInvitationService.sendEmailInvitation(validRequest);
        });
        
        assertEquals("Email is required", exception.getMessage());
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenEmailFormatInvalid() {
        // Given
        validRequest.setEmail("invalid-email");

        // When & Then
        GroupException exception = assertThrows(GroupException.class, () -> {
            groupInvitationService.sendEmailInvitation(validRequest);
        });
        
        assertEquals("Invalid email format", exception.getMessage());
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenCurrentUserNotFound() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(null);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, () -> {
                groupInvitationService.sendEmailInvitation(validRequest);
            });
            
            assertEquals("Unable to identify the current user", exception.getMessage());
        }
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenGroupNotFound() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            when(groupRepository.findById(1)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class, () -> {
                groupInvitationService.sendEmailInvitation(validRequest);
            });
            
            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenGroupNotActive() {
        // Given
        mockGroup.setGroupIsActive(false);
        
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));

            // When & Then
            GroupException exception = assertThrows(GroupException.class, () -> {
                groupInvitationService.sendEmailInvitation(validRequest);
            });
            
            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenCurrentUserNotMember() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(1, 1))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class, () -> {
                groupInvitationService.sendEmailInvitation(validRequest);
            });
            
            assertEquals("You are not an active member of this group", exception.getMessage());
        }
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenCurrentUserNotActiveMember() {
        // Given
        mockCurrentUserMembership.setStatus(GroupMemberStatus.LEFT);
        
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(1, 1))
                    .thenReturn(Optional.of(mockCurrentUserMembership));

            // When & Then
            GroupException exception = assertThrows(GroupException.class, () -> {
                groupInvitationService.sendEmailInvitation(validRequest);
            });
            
            assertEquals("You are not an active member of this group", exception.getMessage());
        }
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenCurrentUserLacksPermission() {
        // Given
        mockCurrentUserMembership.setRole(GroupMemberRole.MEMBER);
        
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(1, 1))
                    .thenReturn(Optional.of(mockCurrentUserMembership));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.MEMBER)).thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, () -> {
                groupInvitationService.sendEmailInvitation(validRequest);
            });
            
            assertEquals("You don't have permission to invite members to this group", exception.getMessage());
        }
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenTargetUserAlreadyActiveMember() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(1, 1))
                    .thenReturn(Optional.of(mockCurrentUserMembership));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.ADMIN)).thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue("target@example.com"))
                    .thenReturn(Optional.of(mockTargetUser));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(2, 1, GroupMemberStatus.ACTIVE))
                    .thenReturn(true);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, () -> {
                groupInvitationService.sendEmailInvitation(validRequest);
            });
            
            assertEquals("User is already a member of this group", exception.getMessage());
        }
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenTargetUserHasPendingInvitation() {
        // Given
        GroupMember pendingMember = new GroupMember();
        pendingMember.setStatus(GroupMemberStatus.PENDING_APPROVAL);
        
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(1, 1))
                    .thenReturn(Optional.of(mockCurrentUserMembership));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.ADMIN)).thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue("target@example.com"))
                    .thenReturn(Optional.of(mockTargetUser));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(2, 1, GroupMemberStatus.ACTIVE))
                    .thenReturn(false);
            when(groupMemberRepository.findByUserIdAndGroupId(2, 1))
                    .thenReturn(Optional.of(pendingMember));

            // When & Then
            GroupException exception = assertThrows(GroupException.class, () -> {
                groupInvitationService.sendEmailInvitation(validRequest);
            });
            
            assertEquals("User already has a pending invitation to this group", exception.getMessage());
        }
    }

    @Test
    void sendEmailInvitation_Success_WhenTargetUserWasPreviouslyRemovedOrLeft() {
        // Given
        GroupMember previousMember = new GroupMember();
        previousMember.setStatus(GroupMemberStatus.LEFT);
        
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            when(groupRepository.findById(1)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(1, 1))
                    .thenReturn(Optional.of(mockCurrentUserMembership));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.ADMIN)).thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue("target@example.com"))
                    .thenReturn(Optional.of(mockTargetUser));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(2, 1, GroupMemberStatus.ACTIVE))
                    .thenReturn(false);
            when(groupMemberRepository.findByUserIdAndGroupId(2, 1))
                    .thenReturn(Optional.of(previousMember));
            when(groupMemberRepository.countActiveGroupMembers(1, GroupMemberStatus.ACTIVE))
                    .thenReturn(3L);
            
            // Mock email service to succeed
            doNothing().when(emailService).sendEmailWithHtmlTemplate(
                    anyString(), anyString(), anyString(), any(Context.class));

            // When
            EmailInvitationResponse result = groupInvitationService.sendEmailInvitation(validRequest);

            // Then
            assertNotNull(result);
            assertTrue(result.isUserExists());
            assertTrue(result.isEmailSent());
            assertEquals("Invitation sent successfully", result.getMessage());
        }
    }
}
