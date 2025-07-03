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
    private BranchLinkService branchLinkService;

    @Mock
    private AppProperties appProperties;

    @Mock
    private InvitationTokenService invitationTokenService;

    @InjectMocks
    private GroupInvitationServiceImpl groupInvitationService;

    private EmailInvitationRequest validRequest;
    private User mockCurrentUser;
    private User mockTargetUser;
    private Group mockGroup;
    private GroupMember mockGroupMember;
    private GroupMember mockInvitation;
    private InvitationTokenData mockTokenData;
    private EmailInvitationResponse mockEmailResponse;
    private GroupInvitationLandingResponse mockLandingResponse;
    private BranchLinkResponse mockBranchLink;
    
    // Test constants
    private static final Integer TEST_GROUP_ID = 1;
    private static final Integer TEST_CURRENT_USER_ID = 100;
    private static final Integer TEST_TARGET_USER_ID = 200;
    private static final String TEST_EMAIL = "target@example.com";
    private static final String TEST_TOKEN = "test-invitation-token-123";
    private static final String TEST_GROUP_NAME = "Test Group";
    private static final String TEST_CURRENT_USER_NAME = "Current User";
    private static final String TEST_TARGET_USER_NAME = "Target User";
    private static final String TEST_BASE_URL = "https://app.example.com";
    private static final String TEST_LAB_NAME = "Test App";
    private static final Long TEST_MEMBER_COUNT = 5L;

    @BeforeEach
    void setUp() {
        // Setup test data
        validRequest = EmailInvitationRequest.builder()
                .groupId(TEST_GROUP_ID)
                .email(TEST_EMAIL)
                .build();

        mockCurrentUser = User.builder()
                .userId(TEST_CURRENT_USER_ID)
                .userEmail("current@example.com")
                .userFullName(TEST_CURRENT_USER_NAME)
                .userIsActive(true)
                .build();

        mockTargetUser = User.builder()
                .userId(TEST_TARGET_USER_ID)
                .userEmail(TEST_EMAIL)
                .userFullName(TEST_TARGET_USER_NAME)
                .userIsActive(true)
                .build();

        mockGroup = new Group();
        mockGroup.setGroupId(TEST_GROUP_ID);
        mockGroup.setGroupName(TEST_GROUP_NAME);
        mockGroup.setGroupIsActive(true);
        mockGroup.setGroupAvatarUrl("https://example.com/avatar.jpg");

        mockGroupMember = new GroupMember();
        mockGroupMember.setUser(mockCurrentUser);
        mockGroupMember.setGroup(mockGroup);
        mockGroupMember.setRole(GroupMemberRole.OWNER);
        mockGroupMember.setStatus(GroupMemberStatus.ACTIVE);

        mockInvitation = new GroupMember();
        mockInvitation.setUser(mockTargetUser);
        mockInvitation.setGroup(mockGroup);
        mockInvitation.setRole(GroupMemberRole.MEMBER);
        mockInvitation.setStatus(GroupMemberStatus.INVITED);

        mockTokenData = InvitationTokenData.builder()
                .groupId(TEST_GROUP_ID)
                .inviterId(TEST_CURRENT_USER_ID)
                .invitedUserId(TEST_TARGET_USER_ID)
                .invitedUserEmail(TEST_EMAIL)
                .status("INVITED")
                .groupName(TEST_GROUP_NAME)
                .inviterName(TEST_CURRENT_USER_NAME)
                .build();

        mockEmailResponse = EmailInvitationResponse.builder()
                .groupId(TEST_GROUP_ID)
                .groupName(TEST_GROUP_NAME)
                .invitedEmail(TEST_EMAIL)
                .emailSent(true)
                .userExists(true)
                .build();

        mockBranchLink = BranchLinkResponse.builder()
                .url("https://branch.link/test")
                .build();
    }

    private void setupAppPropertiesMock() {
        AppProperties.Client clientConfig = new AppProperties.Client();
        clientConfig.setBaseUrl(TEST_BASE_URL);
        when(appProperties.getClient()).thenReturn(clientConfig);
        when(appProperties.getLabName()).thenReturn(TEST_LAB_NAME);
    }

    // ===== SEND EMAIL INVITATION TESTS =====

    @Test
    void sendEmailInvitation_Success_WithValidData() throws Exception {
        // Given
        setupAppPropertiesMock();
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(TEST_CURRENT_USER_ID, TEST_GROUP_ID))
                    .thenReturn(Optional.of(mockGroupMember));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.OWNER)).thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue(TEST_EMAIL))
                    .thenReturn(Optional.of(mockTargetUser));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(TEST_TARGET_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(false);
            when(groupMemberRepository.findByUserIdAndGroupId(TEST_TARGET_USER_ID, TEST_GROUP_ID))
                    .thenReturn(Optional.empty());
            when(groupMemberRepository.save(any(GroupMember.class))).thenReturn(mockInvitation);
            when(invitationTokenService.createInvitationToken(any(InvitationTokenData.class))).thenReturn(TEST_TOKEN);
            when(groupMemberRepository.countActiveGroupMembers(TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(TEST_MEMBER_COUNT);
            doNothing().when(emailService).sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any(Context.class));

            // When
            EmailInvitationResponse result = groupInvitationService.sendEmailInvitation(validRequest);

            // Then
            assertNotNull(result);
            assertEquals(TEST_GROUP_ID, result.getGroupId());
            assertEquals(TEST_GROUP_NAME, result.getGroupName());
            assertEquals(TEST_EMAIL, result.getInvitedEmail());
            assertTrue(result.isEmailSent());
            assertTrue(result.isUserExists());
            assertNotNull(result.getInviteLink());
            assertTrue(result.getInviteLink().contains(TEST_TOKEN));
            
            verify(groupMemberRepository).save(any(GroupMember.class));
            verify(invitationTokenService).createInvitationToken(any(InvitationTokenData.class));
            verify(emailService).sendEmailWithHtmlTemplate(eq(TEST_EMAIL), anyString(), eq("group-invitation"), any(Context.class));
        }
    }

    @Test
    void sendEmailInvitation_Success_UserNotExists() throws Exception {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(TEST_CURRENT_USER_ID, TEST_GROUP_ID))
                    .thenReturn(Optional.of(mockGroupMember));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.OWNER)).thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue(TEST_EMAIL))
                    .thenReturn(Optional.empty());

            // When
            EmailInvitationResponse result = groupInvitationService.sendEmailInvitation(validRequest);

            // Then
            assertNotNull(result);
            assertEquals(TEST_GROUP_ID, result.getGroupId());
            assertEquals(TEST_GROUP_NAME, result.getGroupName());
            assertEquals(TEST_EMAIL, result.getInvitedEmail());
            assertFalse(result.isEmailSent());
            assertFalse(result.isUserExists());
            assertNull(result.getInviteLink());
            assertEquals("User account does not exist. User needs to register an account before joining the group.", result.getMessage());
            
            verify(groupMemberRepository, never()).save(any(GroupMember.class));
            verify(invitationTokenService, never()).createInvitationToken(any(InvitationTokenData.class));
            verify(emailService, never()).sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any(Context.class));
        }
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenRequestIsNull() throws Exception {
        // When & Then
        NullPointerException exception = assertThrows(NullPointerException.class, 
                () -> groupInvitationService.sendEmailInvitation(null));
        
        assertTrue(exception.getMessage().contains("Cannot invoke"));
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenGroupIdIsNull() throws Exception {
        // Given
        validRequest.setGroupId(null);
        
        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
                () -> groupInvitationService.sendEmailInvitation(validRequest));
        
        assertEquals("Group ID is required", exception.getMessage());
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenEmailIsEmpty() throws Exception {
        // Given
        validRequest.setEmail("");
        
        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
                () -> groupInvitationService.sendEmailInvitation(validRequest));
        
        assertEquals("Email is required", exception.getMessage());
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenEmailFormatInvalid() throws Exception {
        // Given
        validRequest.setEmail("invalid-email");
        
        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
                () -> groupInvitationService.sendEmailInvitation(validRequest));
        
        assertEquals("Invalid email format", exception.getMessage());
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenGroupNotFound() throws Exception {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupInvitationService.sendEmailInvitation(validRequest));
            
            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenGroupNotActive() throws Exception {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            mockGroup.setGroupIsActive(false);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupInvitationService.sendEmailInvitation(validRequest));
            
            assertEquals("Group not found", exception.getMessage());
        }
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenUserNotMember() throws Exception {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(TEST_CURRENT_USER_ID, TEST_GROUP_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupInvitationService.sendEmailInvitation(validRequest));
            
            assertEquals("You are not an active member of this group", exception.getMessage());
        }
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenUserLacksPermission() throws Exception {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            mockGroupMember.setRole(GroupMemberRole.MEMBER);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(TEST_CURRENT_USER_ID, TEST_GROUP_ID))
                    .thenReturn(Optional.of(mockGroupMember));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.MEMBER)).thenReturn(false);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupInvitationService.sendEmailInvitation(validRequest));
            
            assertEquals("You don't have permission to invite members to this group", exception.getMessage());
        }
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenUserAlreadyActive() throws Exception {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(TEST_CURRENT_USER_ID, TEST_GROUP_ID))
                    .thenReturn(Optional.of(mockGroupMember));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.OWNER)).thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue(TEST_EMAIL))
                    .thenReturn(Optional.of(mockTargetUser));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(TEST_TARGET_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(true);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupInvitationService.sendEmailInvitation(validRequest));
            
            assertEquals("User is already a member of this group", exception.getMessage());
        }
    }

    @Test
    void sendEmailInvitation_ThrowsException_WhenUserAlreadyInvited() throws Exception {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(TEST_CURRENT_USER_ID, TEST_GROUP_ID))
                    .thenReturn(Optional.of(mockGroupMember));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.OWNER)).thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue(TEST_EMAIL))
                    .thenReturn(Optional.of(mockTargetUser));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(TEST_TARGET_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(false);
            
            GroupMember existingInvitation = new GroupMember();
            existingInvitation.setStatus(GroupMemberStatus.INVITED);
            when(groupMemberRepository.findByUserIdAndGroupId(TEST_TARGET_USER_ID, TEST_GROUP_ID))
                    .thenReturn(Optional.of(existingInvitation));

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                    () -> groupInvitationService.sendEmailInvitation(validRequest));
            
            assertEquals("User has already been invited to this group", exception.getMessage());
        }
    }

    @Test
    void sendEmailInvitation_Success_WhenEmailFailsButTokenCreated() throws Exception {
        // Given
        setupAppPropertiesMock();
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockCurrentUser);
            
            when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));
            when(groupMemberRepository.findByUserIdAndGroupId(TEST_CURRENT_USER_ID, TEST_GROUP_ID))
                    .thenReturn(Optional.of(mockGroupMember));
            when(groupPermissionService.canInviteMembers(GroupMemberRole.OWNER)).thenReturn(true);
            when(userRepository.findByUserEmailAndUserIsActiveTrue(TEST_EMAIL))
                    .thenReturn(Optional.of(mockTargetUser));
            when(groupMemberRepository.existsByUserAndGroupAndStatus(TEST_TARGET_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(false);
            when(groupMemberRepository.findByUserIdAndGroupId(TEST_TARGET_USER_ID, TEST_GROUP_ID))
                    .thenReturn(Optional.empty());
            when(groupMemberRepository.save(any(GroupMember.class))).thenReturn(mockInvitation);
            when(invitationTokenService.createInvitationToken(any(InvitationTokenData.class))).thenReturn(TEST_TOKEN);
            when(groupMemberRepository.countActiveGroupMembers(TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                    .thenReturn(TEST_MEMBER_COUNT);
            doThrow(new RuntimeException("Email service failed"))
                    .when(emailService).sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any(Context.class));

            // When
            EmailInvitationResponse result = groupInvitationService.sendEmailInvitation(validRequest);

            // Then
            assertNotNull(result);
            assertEquals(TEST_GROUP_ID, result.getGroupId());
            assertEquals(TEST_GROUP_NAME, result.getGroupName());
            assertEquals(TEST_EMAIL, result.getInvitedEmail());
            assertFalse(result.isEmailSent());
            assertTrue(result.isUserExists());
            assertNotNull(result.getInviteLink());
            assertTrue(result.getMessage().contains("email delivery failed"));
            
            verify(groupMemberRepository).save(any(GroupMember.class));
            verify(invitationTokenService).createInvitationToken(any(InvitationTokenData.class));
        }
    }

    // ===== PROCESS INVITATION TOKEN TESTS =====

    @Test
    void processInvitationToken_Success_StatusChangeToPendingApproval() throws Exception {
        // Given
        setupAppPropertiesMock();
        when(invitationTokenService.getInvitationTokenData(TEST_TOKEN)).thenReturn(Optional.of(mockTokenData));
        when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));
        when(groupMemberRepository.findByUserIdAndGroupId(TEST_TARGET_USER_ID, TEST_GROUP_ID))
                .thenReturn(Optional.of(mockInvitation));
        when(groupMemberRepository.save(any(GroupMember.class))).thenReturn(mockInvitation);
        when(invitationTokenService.updateInvitationTokenStatus(TEST_TOKEN, "PENDING_APPROVAL")).thenReturn(true);
        when(branchLinkService.createInvitationDeepLink(TEST_GROUP_ID, TEST_TARGET_USER_ID, TEST_CURRENT_USER_ID, "VIEW_GROUP"))
                .thenReturn(mockBranchLink);

        // When
        GroupInvitationLandingResponse result = groupInvitationService.processInvitationToken(TEST_TOKEN);

        // Then
        assertNotNull(result);
        assertEquals(TEST_GROUP_ID, result.getGroupId());
        assertEquals(TEST_GROUP_NAME, result.getGroupName());
        assertEquals(TEST_CURRENT_USER_NAME, result.getInviterName());
        assertEquals(TEST_EMAIL, result.getInvitedEmail());
        assertEquals(ResultType.STATUS_CHANGE_TO_PENDING_APPROVAL, result.getResultType());
        assertTrue(result.isValidInvitation());
        assertEquals("Your request to join the group is now pending approval from group administrators", result.getMessage());
        assertNotNull(result.getJoinButtonLink());
        
        verify(groupMemberRepository).save(argThat(member -> 
            member.getStatus() == GroupMemberStatus.PENDING_APPROVAL));
        verify(invitationTokenService).updateInvitationTokenStatus(TEST_TOKEN, "PENDING_APPROVAL");
    }

    @Test
    void processInvitationToken_Success_AlreadyPendingApproval() throws Exception {
        // Given
        setupAppPropertiesMock();
        mockInvitation.setStatus(GroupMemberStatus.PENDING_APPROVAL);
        when(invitationTokenService.getInvitationTokenData(TEST_TOKEN)).thenReturn(Optional.of(mockTokenData));
        when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));
        when(groupMemberRepository.findByUserIdAndGroupId(TEST_TARGET_USER_ID, TEST_GROUP_ID))
                .thenReturn(Optional.of(mockInvitation));
        when(branchLinkService.createInvitationDeepLink(TEST_GROUP_ID, TEST_TARGET_USER_ID, TEST_CURRENT_USER_ID, "RECHECK_PENDING_STATUS"))
                .thenReturn(mockBranchLink);

        // When
        GroupInvitationLandingResponse result = groupInvitationService.processInvitationToken(TEST_TOKEN);

        // Then
        assertNotNull(result);
        assertEquals(TEST_GROUP_ID, result.getGroupId());
        assertEquals(TEST_GROUP_NAME, result.getGroupName());
        assertEquals(ResultType.ALREADY_PENDING_APPROVAL, result.getResultType());
        assertTrue(result.isValidInvitation());
        assertEquals("Your request is still pending approval. Check your status in the app.", result.getMessage());
        
        verify(groupMemberRepository, never()).save(any(GroupMember.class));
        verify(invitationTokenService, never()).updateInvitationTokenStatus(anyString(), anyString());
    }

    @Test
    void processInvitationToken_Success_AlreadyActiveMember() throws Exception {
        // Given
        setupAppPropertiesMock();
        mockInvitation.setStatus(GroupMemberStatus.ACTIVE);
        when(invitationTokenService.getInvitationTokenData(TEST_TOKEN)).thenReturn(Optional.of(mockTokenData));
        when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));
        when(groupMemberRepository.findByUserIdAndGroupId(TEST_TARGET_USER_ID, TEST_GROUP_ID))
                .thenReturn(Optional.of(mockInvitation));
        when(branchLinkService.createInvitationDeepLink(TEST_GROUP_ID, null, null, "VIEW_GROUP"))
                .thenReturn(mockBranchLink);

        // When
        GroupInvitationLandingResponse result = groupInvitationService.processInvitationToken(TEST_TOKEN);

        // Then
        assertNotNull(result);
        assertEquals(TEST_GROUP_ID, result.getGroupId());
        assertEquals(TEST_GROUP_NAME, result.getGroupName());
        assertEquals(ResultType.ALREADY_ACTIVE_MEMBER, result.getResultType());
        assertTrue(result.isValidInvitation());
        assertEquals("You are already a member of this group. Redirecting to group...", result.getMessage());
        
        verify(groupMemberRepository, never()).save(any(GroupMember.class));
        verify(invitationTokenService, never()).updateInvitationTokenStatus(anyString(), anyString());
    }

    @Test
    void processInvitationToken_ReturnsInvalidResponse_WhenTokenNotFound() throws Exception {
        // Given
        when(invitationTokenService.getInvitationTokenData(TEST_TOKEN)).thenReturn(Optional.empty());

        // When
        GroupInvitationLandingResponse result = groupInvitationService.processInvitationToken(TEST_TOKEN);

        // Then
        assertNotNull(result);
        assertFalse(result.isValidInvitation());
        assertEquals(ResultType.INVALID_OR_USED_TOKEN, result.getResultType());
        assertEquals("This invitation link is invalid or has expired", result.getMessage());
        assertEquals("Invalid Invitation", result.getPageTitle());
    }

    @Test
    void processInvitationToken_ReturnsInvalidResponse_WhenTokenStatusInvalid() throws Exception {
        // Given
        mockTokenData.setStatus("ACCEPTED");
        when(invitationTokenService.getInvitationTokenData(TEST_TOKEN)).thenReturn(Optional.of(mockTokenData));

        // When
        GroupInvitationLandingResponse result = groupInvitationService.processInvitationToken(TEST_TOKEN);

        // Then
        assertNotNull(result);
        assertFalse(result.isValidInvitation());
        assertEquals(ResultType.INVALID_OR_USED_TOKEN, result.getResultType());
        assertEquals("This invitation has already been processed or is invalid", result.getMessage());
    }

    @Test
    void processInvitationToken_ReturnsGroupInactiveResponse_WhenGroupNotFound() throws Exception {
        // Given
        when(invitationTokenService.getInvitationTokenData(TEST_TOKEN)).thenReturn(Optional.of(mockTokenData));
        when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.empty());

        // When
        GroupInvitationLandingResponse result = groupInvitationService.processInvitationToken(TEST_TOKEN);

        // Then
        assertNotNull(result);
        assertFalse(result.isValidInvitation());
        assertEquals(ResultType.GROUP_INACTIVE_OR_DELETED, result.getResultType());
        assertEquals("The group no longer exists", result.getMessage());
        assertEquals("Group No Longer Active", result.getPageTitle());
    }

    @Test
    void processInvitationToken_ReturnsGroupInactiveResponse_WhenGroupNotActive() throws Exception {
        // Given
        mockGroup.setGroupIsActive(false);
        when(invitationTokenService.getInvitationTokenData(TEST_TOKEN)).thenReturn(Optional.of(mockTokenData));
        when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));

        // When
        GroupInvitationLandingResponse result = groupInvitationService.processInvitationToken(TEST_TOKEN);

        // Then
        assertNotNull(result);
        assertFalse(result.isValidInvitation());
        assertEquals(ResultType.GROUP_INACTIVE_OR_DELETED, result.getResultType());
        assertEquals("The group is no longer active", result.getMessage());
    }

    @Test
    void processInvitationToken_ReturnsInvalidResponse_WhenInvitationRecordNotFound() throws Exception {
        // Given
        when(invitationTokenService.getInvitationTokenData(TEST_TOKEN)).thenReturn(Optional.of(mockTokenData));
        when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));
        when(groupMemberRepository.findByUserIdAndGroupId(TEST_TARGET_USER_ID, TEST_GROUP_ID))
                .thenReturn(Optional.empty());

        // When
        GroupInvitationLandingResponse result = groupInvitationService.processInvitationToken(TEST_TOKEN);

        // Then
        assertNotNull(result);
        assertFalse(result.isValidInvitation());
        assertEquals(ResultType.INVALID_OR_USED_TOKEN, result.getResultType());
        assertEquals("Invitation record not found", result.getMessage());
    }

    @Test
    void processInvitationToken_ReturnsInvalidResponse_WhenInvitationStatusInvalid() throws Exception {
        // Given
        mockInvitation.setStatus(GroupMemberStatus.LEFT);
        when(invitationTokenService.getInvitationTokenData(TEST_TOKEN)).thenReturn(Optional.of(mockTokenData));
        when(groupRepository.findById(TEST_GROUP_ID)).thenReturn(Optional.of(mockGroup));
        when(groupMemberRepository.findByUserIdAndGroupId(TEST_TARGET_USER_ID, TEST_GROUP_ID))
                .thenReturn(Optional.of(mockInvitation));

        // When
        GroupInvitationLandingResponse result = groupInvitationService.processInvitationToken(TEST_TOKEN);

        // Then
        assertNotNull(result);
        assertFalse(result.isValidInvitation());
        assertEquals(ResultType.INVALID_OR_USED_TOKEN, result.getResultType());
        assertEquals("Invitation status is invalid", result.getMessage());
    }

    @Test
    void processInvitationToken_ReturnsInvalidResponse_WhenExceptionThrown() throws Exception {
        // Given
        when(invitationTokenService.getInvitationTokenData(TEST_TOKEN))
                .thenThrow(new RuntimeException("Database error"));

        // When
        GroupInvitationLandingResponse result = groupInvitationService.processInvitationToken(TEST_TOKEN);

        // Then
        assertNotNull(result);
        assertFalse(result.isValidInvitation());
        assertEquals(ResultType.INVALID_OR_USED_TOKEN, result.getResultType());
        assertEquals("Failed to process invitation", result.getMessage());
    }

    // ===== HELPER METHODS =====

    private User createTestUser(Integer userId, String email, String fullName) {
        return User.builder()
                .userId(userId)
                .userEmail(email)
                .userFullName(fullName)
                .userIsActive(true)
                .build();
    }

    private Group createTestGroup(Integer groupId, String groupName, boolean isActive) {
        Group group = new Group();
        group.setGroupId(groupId);
        group.setGroupName(groupName);
        group.setGroupIsActive(isActive);
        return group;
    }

    private GroupMember createGroupMember(Group group, User user, GroupMemberRole role, GroupMemberStatus status) {
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setRole(role);
        member.setStatus(status);
        member.setJoinDate(LocalDateTime.now());
        return member;
    }

    private InvitationTokenData createTestTokenData(Integer groupId, Integer inviterId, Integer invitedUserId, String status) {
        return InvitationTokenData.builder()
                .groupId(groupId)
                .inviterId(inviterId)
                .invitedUserId(invitedUserId)
                .invitedUserEmail(TEST_EMAIL)
                .status(status)
                .groupName(TEST_GROUP_NAME)
                .inviterName(TEST_CURRENT_USER_NAME)
                .build();
    }
}
