package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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
import vn.fpt.seima.seimaserver.service.*;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of GroupInvitationService
 * Follows Single Responsibility Principle and is designed for testability
 */
@Service
@RequiredArgsConstructor
public class GroupInvitationServiceImpl implements GroupInvitationService {

    private static final Logger logger = LoggerFactory.getLogger(GroupInvitationServiceImpl.class);

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final GroupPermissionService groupPermissionService;
    private final BranchLinkService branchLinkService;
    private final AppProperties appProperties;
    private final InvitationTokenService invitationTokenService;
    private final NotificationService notificationService;



    private GroupMember createInvitedGroupMembership(Group group, User user) {
        GroupMember groupMember = new GroupMember();
        groupMember.setGroup(group);
        groupMember.setUser(user);
        groupMember.setRole(GroupMemberRole.MEMBER);
        groupMember.setStatus(GroupMemberStatus.INVITED); // Status = INVITED (được mời)
        groupMember.setJoinDate(LocalDateTime.now());

        GroupMember savedMember = groupMemberRepository.save(groupMember);
        logger.info("Created invitation record with INVITED status for user {} in group {}",
                user.getUserId(), group.getGroupId());

        return savedMember;
    }

    /**
     * Create invitation token in Redis with 30-day expiration
     * Contains all invitation details for tracking and validation
     */
    private String createInvitationToken(Group group, User inviter, User invitedUser) {
        try {
            InvitationTokenData tokenData = InvitationTokenData.builder()
                    .groupId(group.getGroupId())
                    .inviterId(inviter.getUserId())
                    .invitedUserId(invitedUser.getUserId())
                    .invitedUserEmail(invitedUser.getUserEmail())
                    .status("INVITED")
                    .groupName(group.getGroupName())
                    .inviterName(inviter.getUserFullName())
                    .build();

            String token = invitationTokenService.createInvitationToken(tokenData);
            logger.info("Successfully created invitation token for user {} to join group {}",
                    invitedUser.getUserId(), group.getGroupId());

            return token;

        } catch (Exception e) {
            logger.error("Failed to create invitation token for user {} to join group {}",
                    invitedUser.getUserId(), group.getGroupId(), e);
            // Don't fail the entire invitation process if token creation fails
            // This is for tracking purposes only
            return null;
        }
    }
    

    private String buildInviteLinkWithToken(String invitationToken) {
        if (invitationToken == null) {
            return null;
        }

        String baseUrl = appProperties.getClient().getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            logger.warn("Client base URL not configured, returning token only");
            return invitationToken;
        }

        // Remove trailing slash if exists
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        // Return invite link using invitation token
        return String.format("%s/invite/%s", baseUrl, invitationToken);
    }
    
    @Override
    @Transactional
    public EmailInvitationResponse sendEmailInvitation(EmailInvitationRequest request) {
        logger.info("Processing email invitation for group: {} to email: {}", request.getGroupId(), request.getEmail());
        
        // Validate request
        validateEmailInvitationRequest(request);
        
        // Get current user (inviter)
        User currentUser = UserUtils.getCurrentUser();
        
        // Validate group and inviter permissions
        Group group = validateGroupAndInviterPermissions(request.getGroupId(), currentUser);
        
        // Check if target user exists
        Optional<User> targetUserOpt = userRepository.findByUserEmailAndUserIsActiveTrue(request.getEmail());
        
        if (targetUserOpt.isEmpty()) {
            logger.warn("User with email {} not found or inactive", request.getEmail());
            return buildUserNotExistsResponse(request, group);
        }
        
        User targetUser = targetUserOpt.get();
        
        // Check if user is already a member
        validateTargetUserMembership(targetUser.getUserId(), group.getGroupId());

        // Create GroupMember record with INVITED status
        // This represents that the user has been invited but hasn't joined the group yet
        GroupMember invitedMember = createInvitedGroupMembership(group, targetUser);
        logger.info("Created invitation record for user {} in group {} with status INVITED",
                targetUser.getUserId(), group.getGroupId());

        // Create invitation token in Redis (expires in 30 days)
        String invitationToken = createInvitationToken(group, currentUser, targetUser);
        logger.info("Created invitation token in Redis for user {} to join group {}",
                targetUser.getUserId(), group.getGroupId());
        
        // Send email invitation
        boolean emailSent = sendInvitationEmail(request, group, currentUser, targetUser, invitationToken);
        
        // Build response
        String inviteLink = buildInviteLinkWithToken(invitationToken);
        
        EmailInvitationResponse response = EmailInvitationResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .invitedEmail(request.getEmail())
                .inviteLink(inviteLink)
                .emailSent(emailSent)
                .userExists(true)
                .message(buildInvitationResponseMessage(emailSent, invitationToken))
                .build();

        logger.info("Email invitation processed for group: {} to email: {} - emailSent: {}, memberStatus: INVITED, tokenCreated: {}",
                request.getGroupId(), request.getEmail(), emailSent, invitationToken != null);
        
        return response;
    }
    
    /**
     * Validate email invitation request
     */
    private void validateEmailInvitationRequest(EmailInvitationRequest request) {
        if (request == null) {
            throw new GroupException("Email invitation request cannot be null");
        }
        
        if (request.getGroupId() == null) {
            throw new GroupException("Group ID is required");
        }
        
        if (!StringUtils.hasText(request.getEmail())) {
            throw new GroupException("Email is required");
        }
        
        // Basic email format validation (additional to @Email annotation)
        String email = request.getEmail().trim().toLowerCase();
        if (!email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")) {
            throw new GroupException("Invalid email format");
        }
    }
    
    /**
     * Validate group exists, is active, and current user has permission to invite
     */
    private Group validateGroupAndInviterPermissions(Integer groupId, User currentUser) {
        // Find and validate group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException("Group not found"));
        
        // Check if group is active
        if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
            throw new GroupException("Group not found");
        }
        
        // Check current user's membership and role
        Optional<GroupMember> currentUserMembership = groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                currentUser.getUserId(), groupId);
        
        if (currentUserMembership.isEmpty() || 
            currentUserMembership.get().getStatus() != GroupMemberStatus.ACTIVE) {
            throw new GroupException("You are not an active member of this group");
        }
        
        GroupMemberRole currentUserRole = currentUserMembership.get().getRole();
        
        // Use permission service to check if user can invite members
        if (!groupPermissionService.canInviteMembers(currentUserRole)) {
            throw new GroupException("You don't have permission to invite members to this group");
        }
        
        return group;
    }
    
    /**
     * Validate target user is not already a member
     */
    private void validateTargetUserMembership(Integer targetUserId, Integer groupId) {
        // Check if user is already an active member
        if (groupMemberRepository.existsByUserAndGroupAndStatus(targetUserId, groupId, GroupMemberStatus.ACTIVE)) {
            throw new GroupException("User is already a member of this group");
        }
        
        // Check if user was previously a member and handle gracefully
        // Use the method that returns the most recent membership to handle multiple records
        Optional<GroupMember> mostRecentMembership = groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(targetUserId, groupId);

        if (mostRecentMembership.isPresent()) {
            GroupMemberStatus status = mostRecentMembership.get().getStatus();

            if (status == GroupMemberStatus.PENDING_APPROVAL) {
                throw new GroupException("User already has a pending invitation to this group");
            }
            if (status == GroupMemberStatus.INVITED) {
                throw new GroupException("User has already been invited to this group");
            }
            // If status is LEFT or REJECTED, they can be invited again - no exception thrown
        }
    }
    
    /**
     * Send invitation email to target user
     */
    private boolean sendInvitationEmail(EmailInvitationRequest request, Group group, User inviter, User targetUser, String invitationToken) {
        try {
            // Get member count
            Long memberCount = groupMemberRepository.countActiveGroupMembers(
                    group.getGroupId(), GroupMemberStatus.ACTIVE);

            // Build invite link using invitation token instead of invite code
            String inviteLink = buildInviteLinkWithToken(invitationToken);
            
            // Prepare email context
            Context context = new Context();
            context.setVariable("inviterName", inviter.getUserFullName());
            context.setVariable("groupName", group.getGroupName());
            context.setVariable("groupAvatarUrl", group.getGroupAvatarUrl());
            context.setVariable("memberCount", memberCount.intValue());
            context.setVariable("inviteLink", inviteLink);
            context.setVariable("appName", appProperties.getLabName());
            

            
            // Send email
            String subject = String.format("Group invitation to '%s' on %s", 
                    group.getGroupName(), appProperties.getLabName());
            
            emailService.sendEmailWithHtmlTemplate(
                    targetUser.getUserEmail(),
                    subject,
                    "group-invitation",
                    context
            );

            logger.info("Invitation email sent successfully to: {} with token link", targetUser.getUserEmail());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send invitation email to: {} for group: {}", 
                    targetUser.getUserEmail(), group.getGroupId(), e);
            return false;
        }
    }
    
    /**
     * Build response when user doesn't exist
     */
    private EmailInvitationResponse buildUserNotExistsResponse(EmailInvitationRequest request, Group group) {
        return EmailInvitationResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .invitedEmail(request.getEmail())
                .inviteLink(null)
                .emailSent(false)
                .userExists(false)
                .message("User account does not exist. User needs to register an account before joining the group.")
                .build();
    }

    /**
     * Build invitation response message based on email and token creation status
     */
    private String buildInvitationResponseMessage(boolean emailSent, String invitationToken) {
        StringBuilder message = new StringBuilder();

        if (emailSent && invitationToken != null) {
            message.append("The invitation has been sent successfully.");
        } else if (emailSent && invitationToken == null) {
            message.append("The invitation has been sent successfully.");
        } else if (!emailSent && invitationToken != null) {
            message.append("Invitation created but email delivery failed.");
        } else {
            message.append("Invitation created but email delivery failed.");
        }

        return message.toString();
    }



    @Override
    @Transactional
    public GroupInvitationLandingResponse processInvitationToken(String invitationToken) {
        logger.info("Processing invitation token: {}", invitationToken);

        try {
            // Get invitation token data
            Optional<InvitationTokenData> tokenDataOpt = invitationTokenService.getInvitationTokenData(invitationToken);

            if (tokenDataOpt.isEmpty()) {
                logger.warn("Invalid or expired invitation token: {}", invitationToken);
                return buildInvalidTokenResponse(invitationToken, "This invitation link is invalid or has expired");
            }

            InvitationTokenData tokenData = tokenDataOpt.get();

            // Check if token status is valid for processing
            if (!"INVITED".equals(tokenData.getStatus()) && !"PENDING_APPROVAL".equals(tokenData.getStatus())) {
                logger.warn("Invalid token status for processing: {} - status: {}", invitationToken, tokenData.getStatus());
                return buildInvalidTokenResponse(invitationToken, "This invitation has already been processed or is invalid");
            }

            // Find and validate group
            Optional<Group> groupOpt = groupRepository.findById(tokenData.getGroupId());

            if (groupOpt.isEmpty()) {
                logger.warn("Group not found for token: {} - groupId: {}", invitationToken, tokenData.getGroupId());
                return buildGroupInactiveResponse(invitationToken, "The group no longer exists");
            }

            Group group = groupOpt.get();

            if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
                logger.warn("Group is not active for token: {} - groupId: {}", invitationToken, tokenData.getGroupId());
                return buildGroupInactiveResponse(invitationToken, "The group is no longer active");
            }

            // Find and validate group member invitation
            Optional<GroupMember> invitationOpt = groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                    tokenData.getInvitedUserId(), tokenData.getGroupId());

            if (invitationOpt.isEmpty()) {
                logger.warn("No invitation record found for token: {} - userId: {}, groupId: {}",
                        invitationToken, tokenData.getInvitedUserId(), tokenData.getGroupId());
                return buildInvalidTokenResponse(invitationToken, "Invitation record not found");
            }

            GroupMember invitation = invitationOpt.get();

            // Handle different invitation statuses
            if (invitation.getStatus() == GroupMemberStatus.PENDING_APPROVAL) {
                logger.info("User already has pending approval status for token: {}", invitationToken);
                return buildPendingApprovalResponse(tokenData, group);
            }

            if (invitation.getStatus() == GroupMemberStatus.ACTIVE) {
                logger.info("User is already an active member of the group for token: {}", invitationToken);
                return buildActiveUserResponse(tokenData, group);
            }

            if (invitation.getStatus() != GroupMemberStatus.INVITED) {
                logger.warn("Invalid invitation status in database for token: {} - status: {}",
                        invitationToken, invitation.getStatus());
                return buildInvalidTokenResponse(invitationToken, "Invitation status is invalid");
            }

            // Update status from INVITED to PENDING_APPROVAL
            invitation.setStatus(GroupMemberStatus.PENDING_APPROVAL);
            groupMemberRepository.save(invitation);

            // Update token status in Redis
            boolean tokenUpdated = invitationTokenService.updateInvitationTokenStatus(invitationToken, "PENDING_APPROVAL");



            // Gửi notification cho chính bản thân báo pending approval
            try {
                notificationService.sendPendingApprovalNotificationToUser(
                    tokenData.getGroupId(),
                    tokenData.getInvitedUserId(),
                    tokenData.getGroupName()
                );
            } catch (Exception e) {
                logger.error("Failed to send pending approval notification to user", e);
                // Không fail toàn bộ process nếu notification fail
            }

            // Gửi notification cho admin và owner
            try {
                // Lấy user name của người yêu cầu join
                String invitedUserName = invitation.getUser().getUserFullName();
                
                notificationService.sendGroupJoinRequestNotification(
                    tokenData.getGroupId(), 
                    tokenData.getInvitedUserId(), 
                    invitedUserName
                );
            } catch (Exception e) {
                logger.error("Failed to send group join request notification", e);
                // Không fail toàn bộ process nếu notification fail
            }

            logger.info("Successfully processed invitation token: {} - updated status to PENDING_APPROVAL, tokenUpdated: {}",
                    invitationToken, tokenUpdated);

            // Build successful landing response
            return buildSuccessfulTokenResponse(tokenData, group);

        } catch (Exception e) {
            logger.error("Error processing invitation token: {}", invitationToken, e);
            return buildInvalidTokenResponse(invitationToken, "Failed to process invitation");
        }
    }

    /**
     * Build successful token response after processing
     */
    private GroupInvitationLandingResponse buildSuccessfulTokenResponse(InvitationTokenData tokenData, Group group) {
        // Create Branch Deep Link for invitation with VIEW_GROUP action
        BranchLinkResponse branchLinkResponse = branchLinkService.createInvitationDeepLink(
                tokenData.getGroupId(),
                tokenData.getInvitedUserId(),
                tokenData.getInviterId(),
                "VIEW_GROUP"
        );

        return GroupInvitationLandingResponse.builder()
                .groupId(tokenData.getGroupId())
                .groupName(tokenData.getGroupName())
                .inviterName(tokenData.getInviterName())
                .invitedEmail(tokenData.getInvitedUserEmail())
                .pageTitle(String.format("Join \"%s\" group", tokenData.getGroupName()))
                .pageDescription(String.format("Your request to join \"%s\" group is now pending approval", tokenData.getGroupName()))
                .joinButtonLink(branchLinkResponse.getUrl()) // Branch Deep Link for redirect
                .appName(appProperties.getLabName())
                .appDownloadUrl(appProperties.getClient().getBaseUrl())
                .isValidInvitation(true)
                .message("Your request to join the group is now pending approval from group administrators")
                .resultType(ResultType.STATUS_CHANGE_TO_PENDING_APPROVAL)
                .build();
    }

    /**
     * Build pending approval response for users who already have pending status
     */
    private GroupInvitationLandingResponse buildPendingApprovalResponse(InvitationTokenData tokenData, Group group) {
        // Create Branch Deep Link with RECHECK_PENDING_STATUS action
        BranchLinkResponse branchLinkResponse = branchLinkService.createInvitationDeepLink(
                tokenData.getGroupId(),
                tokenData.getInvitedUserId(),
                tokenData.getInviterId(),
                "RECHECK_PENDING_STATUS"
        );

        return GroupInvitationLandingResponse.builder()
                .groupId(tokenData.getGroupId())
                .groupName(tokenData.getGroupName())
                .inviterName(tokenData.getInviterName())
                .invitedEmail(tokenData.getInvitedUserEmail())
                .pageTitle(String.format("Pending: \"%s\" group", tokenData.getGroupName()))
                .pageDescription(String.format("Your request to join \"%s\" group is still pending approval", tokenData.getGroupName()))
                .joinButtonLink(branchLinkResponse.getUrl()) // Branch Deep Link for recheck status
                .appName(appProperties.getLabName())
                .appDownloadUrl(appProperties.getClient().getBaseUrl())
                .isValidInvitation(true)
                .message("Your request is still pending approval. Check your status in the app.")
                .resultType(ResultType.ALREADY_PENDING_APPROVAL)
                .build();
    }

    /**
     * Build active user response for users who are already active members
     */
    private GroupInvitationLandingResponse buildActiveUserResponse(InvitationTokenData tokenData, Group group) {
        // Create Branch Deep Link with VIEW_GROUP action (no need for invitedUserId/inviterId since user is already a member)
        BranchLinkResponse branchLinkResponse = branchLinkService.createInvitationDeepLink(
                tokenData.getGroupId(),
                null, // No need for invitedUserId since user is already a member
                null, // No need for inviterId since user is already a member
                "VIEW_GROUP"
        );

        return GroupInvitationLandingResponse.builder()
                .groupId(tokenData.getGroupId())
                .groupName(tokenData.getGroupName())
                .inviterName(tokenData.getInviterName())
                .invitedEmail(tokenData.getInvitedUserEmail())
                .pageTitle(String.format("Welcome back to \"%s\"", tokenData.getGroupName()))
                .pageDescription(String.format("You are already a member of \"%s\" group", tokenData.getGroupName()))
                .joinButtonLink(branchLinkResponse.getUrl()) // Branch Deep Link for redirect
                .appName(appProperties.getLabName())
                .appDownloadUrl(appProperties.getClient().getBaseUrl())
                .isValidInvitation(true)
                .message("You are already a member of this group. Redirecting to group...")
                .resultType(ResultType.ALREADY_ACTIVE_MEMBER)
                .build();
    }

    /**
     * Build invalid token response
     */
    private GroupInvitationLandingResponse buildInvalidTokenResponse(String token, String message) {
        return GroupInvitationLandingResponse.builder()
                .inviteCode(token)
                .isValidInvitation(false)
                .message(message)
                .pageTitle("Invalid Invitation")
                .pageDescription("This invitation link is not valid or has expired")
                .resultType(ResultType.INVALID_OR_USED_TOKEN)
                .build();
    }

    /**
     * Build group inactive response for deactivated or deleted groups
     */
    private GroupInvitationLandingResponse buildGroupInactiveResponse(String token, String message) {
        return GroupInvitationLandingResponse.builder()
                .inviteCode(token)
                .isValidInvitation(false)
                .message(message)
                .pageTitle("Group No Longer Active")
                .pageDescription("Sorry, this group is no longer active")
                .resultType(ResultType.GROUP_INACTIVE_OR_DELETED)
                .build();
    }
}
