package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Group Invitation Service Tests")
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

    // Test data
    private User currentUser;
    private User targetUser;
    private Group group;
    private GroupMember currentUserMembership;
    private EmailInvitationRequest request;

    @BeforeEach
    void setUp() {
        // Setup current user (inviter)
        currentUser = User.builder()
                .userId(1)
                .userFullName("John Doe")
                .userEmail("john@example.com")
                .userIsActive(true)
                .build();

        // Setup target user (invitee)
        targetUser = User.builder()
                .userId(2)
                .userFullName("Jane Smith")
                .userEmail("jane@example.com")
                .userIsActive(true)
                .build();

        // Setup group
        group = Group.builder()
                .groupId(1)
                .groupName("Test Group")
                .groupInviteCode("testcode123")
                .groupIsActive(true)
                .groupAvatarUrl("https://example.com/avatar.jpg")
                .groupCreatedDate(LocalDateTime.now())
                .build();

        // Setup current user membership
        currentUserMembership = GroupMember.builder()
                .user(currentUser)
                .group(group)
                .role(GroupMemberRole.ADMIN)
                .status(GroupMemberStatus.ACTIVE)
                .joinDate(LocalDateTime.now())
                .build();

        // Setup request
        request = EmailInvitationRequest.builder()
                .groupId(1)
                .email("jane@example.com")
                .build();

        // Setup app properties
        when(appProperties.getLabName()).thenReturn("Seima Lab");
    }

    @Test
    @DisplayName("Should send email invitation successfully when all conditions are met")
    void shouldSendEmailInvitationSuccessfully() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);

            when(groupRepository.findById(1)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(1, 1))
                    .thenReturn(Optional.of(currentUserMembership));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.ADMIN)).thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue("jane@example.com"))
                    .thenReturn(Optional.of(targetUser));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(2, 1, GroupMemberStatus.ACTIVE))
                    .thenReturn(false);
            when(groupMemberRepository.findByUserIdAndGroupId(2, 1))
                    .thenReturn(Optional.empty());
            when(groupMemberRepository.countActiveGroupMembers(1, GroupMemberStatus.ACTIVE))
                    .thenReturn(5L);

            doNothing().when(emailService).sendEmailWithHtmlTemplate(
                    anyString(), anyString(), anyString(), any(Context.class));

            // When
            EmailInvitationResponse response = groupInvitationService.sendEmailInvitation(request);

            // Then
            assertNotNull(response);
            assertEquals(1, response.getGroupId());
            assertEquals("Test Group", response.getGroupName());
            assertEquals("jane@example.com", response.getInvitedEmail());
            assertTrue(response.isEmailSent());
            assertTrue(response.isUserExists());
            assertEquals("Invitation sent successfully", response.getMessage());
            assertNotNull(response.getInviteLink());

            // Verify email service was called
            verify(emailService).sendEmailWithHtmlTemplate(
                    eq("jane@example.com"),
                    eq("Group invitation to 'Test Group' on Seima Lab"),
                    eq("group-invitation"),
                    any(Context.class)
            );
        }
    }

    @Test
    @DisplayName("Should return user not exists response when target user is not found")
    void shouldReturnUserNotExistsWhenTargetUserNotFound() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);

            when(groupRepository.findById(1)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(1, 1))
                    .thenReturn(Optional.of(currentUserMembership));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.ADMIN)).thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue("jane@example.com"))
                    .thenReturn(Optional.empty());

            // When
            EmailInvitationResponse response = groupInvitationService.sendEmailInvitation(request);

            // Then
            assertNotNull(response);
            assertEquals(1, response.getGroupId());
            assertEquals("Test Group", response.getGroupName());
            assertEquals("jane@example.com", response.getInvitedEmail());
            assertFalse(response.isEmailSent());
            assertFalse(response.isUserExists());
            assertNull(response.getInviteLink());
            assertEquals("User account does not exist. User needs to register an account before joining the group.", 
                    response.getMessage());

            // Verify email service was never called
            verify(emailService, never()).sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any(Context.class));
        }
    }

    @Test
    @DisplayName("Should throw exception when request is null")
    void shouldThrowExceptionWhenRequestIsNull() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
                () -> groupInvitationService.sendEmailInvitation(null));
        
        assertEquals("Email invitation request cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when group ID is null")
    void shouldThrowExceptionWhenGroupIdIsNull() {
        // Given
        request.setGroupId(null);

        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
                () -> groupInvitationService.sendEmailInvitation(request));
        
        assertEquals("Group ID is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when email is blank")
    void shouldThrowExceptionWhenEmailIsBlank() {
        // Given
        request.setEmail("   ");

        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
                () -> groupInvitationService.sendEmailInvitation(request));
        
        assertEquals("Email is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when email format is invalid")
    void shouldThrowExceptionWhenEmailFormatIsInvalid() {
        // Given
        request.setEmail("invalid-email");

        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
                () -> groupInvitationService.sendEmailInvitation(request));
        
        assertEquals("Invalid email format", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when group not found")
    void shouldThrowExceptionWhenGroupNotFound() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);

            when(groupRepository.findById(1)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupInvitationService.sendEmailInvitation(request));
            
            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    @DisplayName("Should throw exception when group is not active")
    void shouldThrowExceptionWhenGroupIsNotActive() {
        // Given
        group.setGroupIsActive(false);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);

            when(groupRepository.findById(1)).thenReturn(Optional.of(group));

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupInvitationService.sendEmailInvitation(request));
            
            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    @DisplayName("Should throw exception when current user is not active member")
    void shouldThrowExceptionWhenCurrentUserIsNotActiveMember() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);

            when(groupRepository.findById(1)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(1, 1))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupInvitationService.sendEmailInvitation(request));
            
            assertEquals("You are not an active member of this group", exception.getMessage());
        }
    }

    @Test
    @DisplayName("Should throw exception when current user doesn't have permission to invite")
    void shouldThrowExceptionWhenCurrentUserDoesNotHavePermissionToInvite() {
        // Given
        currentUserMembership.setRole(GroupMemberRole.MEMBER);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);

            when(groupRepository.findById(1)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(1, 1))
                    .thenReturn(Optional.of(currentUserMembership));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.MEMBER)).thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupInvitationService.sendEmailInvitation(request));
            
            assertEquals("You don't have permission to invite members to this group", exception.getMessage());
        }
    }

    @Test
    @DisplayName("Should throw exception when target user is already active member")
    void shouldThrowExceptionWhenTargetUserIsAlreadyActiveMember() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);

            when(groupRepository.findById(1)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(1, 1))
                    .thenReturn(Optional.of(currentUserMembership));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.ADMIN)).thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue("jane@example.com"))
                    .thenReturn(Optional.of(targetUser));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(2, 1, GroupMemberStatus.ACTIVE))
                    .thenReturn(true);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupInvitationService.sendEmailInvitation(request));
            
            assertEquals("User is already a member of this group", exception.getMessage());
        }
    }

    @Test
    @DisplayName("Should throw exception when target user has pending invitation")
    void shouldThrowExceptionWhenTargetUserHasPendingInvitation() {
        // Given
        GroupMember pendingMembership = GroupMember.builder()
                .user(targetUser)
                .group(group)
                .status(GroupMemberStatus.PENDING)
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);

            when(groupRepository.findById(1)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(1, 1))
                    .thenReturn(Optional.of(currentUserMembership));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.ADMIN)).thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue("jane@example.com"))
                    .thenReturn(Optional.of(targetUser));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(2, 1, GroupMemberStatus.ACTIVE))
                    .thenReturn(false);
            when(groupMemberRepository.findByUserIdAndGroupId(2, 1))
                    .thenReturn(Optional.of(pendingMembership));

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupInvitationService.sendEmailInvitation(request));
            
            assertEquals("User already has a pending invitation to this group", exception.getMessage());
        }
    }

    @Test
    @DisplayName("Should return email not sent when email service fails")
    void shouldReturnEmailNotSentWhenEmailServiceFails() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);

            when(groupRepository.findById(1)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(1, 1))
                    .thenReturn(Optional.of(currentUserMembership));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.ADMIN)).thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue("jane@example.com"))
                    .thenReturn(Optional.of(targetUser));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(2, 1, GroupMemberStatus.ACTIVE))
                    .thenReturn(false);
            when(groupMemberRepository.findByUserIdAndGroupId(2, 1))
                    .thenReturn(Optional.empty());
            when(groupMemberRepository.countActiveGroupMembers(1, GroupMemberStatus.ACTIVE))
                    .thenReturn(5L);

            doThrow(new RuntimeException("Email service error"))
                    .when(emailService).sendEmailWithHtmlTemplate(
                            anyString(), anyString(), anyString(), any(Context.class));

            // When
            EmailInvitationResponse response = groupInvitationService.sendEmailInvitation(request);

            // Then
            assertNotNull(response);
            assertEquals(1, response.getGroupId());
            assertEquals("Test Group", response.getGroupName());
            assertEquals("jane@example.com", response.getInvitedEmail());
            assertFalse(response.isEmailSent());
            assertTrue(response.isUserExists());
            assertEquals("Failed to send invitation email", response.getMessage());
            assertNotNull(response.getInviteLink());
        }
    }

    @Test
    @DisplayName("Should send invitation to user who previously left the group")
    void shouldSendInvitationToUserWhoPreviouslyLeftTheGroup() {
        // Given
        GroupMember leftMembership = GroupMember.builder()
                .user(targetUser)
                .group(group)
                .status(GroupMemberStatus.LEFT)
                .build();

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);

            when(groupRepository.findById(1)).thenReturn(Optional.of(group));
            when(groupMemberRepository.findByUserIdAndGroupId(1, 1))
                    .thenReturn(Optional.of(currentUserMembership));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.ADMIN)).thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue("jane@example.com"))
                    .thenReturn(Optional.of(targetUser));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(2, 1, GroupMemberStatus.ACTIVE))
                    .thenReturn(false);
            when(groupMemberRepository.findByUserIdAndGroupId(2, 1))
                    .thenReturn(Optional.of(leftMembership));
            when(groupMemberRepository.countActiveGroupMembers(1, GroupMemberStatus.ACTIVE))
                    .thenReturn(5L);

            doNothing().when(emailService).sendEmailWithHtmlTemplate(
                    anyString(), anyString(), anyString(), any(Context.class));

            // When
            EmailInvitationResponse response = groupInvitationService.sendEmailInvitation(request);

            // Then
            assertNotNull(response);
            assertTrue(response.isEmailSent());
            assertTrue(response.isUserExists());
            assertEquals("Invitation sent successfully", response.getMessage());

            // Verify email service was called
            verify(emailService).sendEmailWithHtmlTemplate(
                    eq("jane@example.com"),
                    anyString(),
                    eq("group-invitation"),
                    any(Context.class)
            );
        }
    }
} 