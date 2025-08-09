package vn.fpt.seima.seimaserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import vn.fpt.seima.seimaserver.config.base.AppProperties;
import vn.fpt.seima.seimaserver.dto.request.group.AcceptGroupMemberRequest;
import vn.fpt.seima.seimaserver.dto.request.group.RejectGroupMemberRequest;
import vn.fpt.seima.seimaserver.dto.request.group.TransferOwnershipRequest;
import vn.fpt.seima.seimaserver.dto.request.group.UpdateMemberRoleRequest;
import vn.fpt.seima.seimaserver.dto.response.group.*;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.exception.GroupException;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.repository.GroupRepository;
import vn.fpt.seima.seimaserver.service.*;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service

public class GroupMemberServiceImpl implements GroupMemberService {

    @Autowired
    private  GroupMemberRepository groupMemberRepository;
    @Autowired
    private  GroupRepository groupRepository;
    @Autowired
    private  GroupPermissionService groupPermissionService;
    @Autowired
    private  GroupValidationService groupValidationService;
    @Autowired
    private  InvitationTokenService invitationTokenService;
    @Autowired
    private  NotificationService notificationService;
    @Autowired
    private  EmailService emailService;
    @Autowired
    private  AppProperties appProperties;

    @Value("${app.email.group-rejection.html-template}")
    private String groupRejectionHtmlTemplate;

    @Value("${app.email.group-rejection.subject}")
    private String groupRejectionSubject;

    @Value("${app.email.group-acceptance.html-template}")
    private String groupAcceptanceHtmlTemplate;

    @Value("${app.email.group-acceptance.subject}")
    private String groupAcceptanceSubject;

    @Value("${app.email.group-role-update.html-template}")
    private String groupRoleUpdateHtmlTemplate;

    @Value("${app.email.group-role-update.subject}")
    private String groupRoleUpdateSubject;

    @Value("${app.email.group-role-update-notification.html-template}")
    private String groupRoleUpdateNotificationHtmlTemplate;

    @Value("${app.email.group-role-update-notification.subject}")
    private String groupRoleUpdateNotificationSubject;

    @Override
    @Transactional(readOnly = true)
    public GroupMemberListResponse getActiveGroupMembers(Integer groupId) {
        log.info("Getting active members for group ID: {}", groupId);

        // Validate input
        if (groupId == null) {
            throw new GroupException("Group ID cannot be null");
        }

        // Get current user
        User currentUser = getCurrentUser();

        // Find and validate group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException("Group not found"));

        // Check if group is active
        if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
            throw new GroupException("Group not found");
        }

        // Check if current user is an active member
        if (!groupMemberRepository.existsByUserAndGroupAndStatus(
                currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE)) {
            throw new GroupException("You don't have permission to view this group's members");
        }

        // Get group leader (owner)
        GroupMember leader = groupMemberRepository.findGroupOwner(
                groupId, GroupMemberStatus.ACTIVE)
                .orElseThrow(() -> new GroupException("Group owner not found for group ID: " + groupId));

        // Check if leader account is still active
        if (!Boolean.TRUE.equals(leader.getUser().getUserIsActive())) {
            throw new GroupException("Group owner account is no longer active");
        }

        GroupMemberResponse leaderResponse = mapToGroupMemberResponse(leader);

        // Get all active members
        List<GroupMember> allMembers = groupMemberRepository.findActiveGroupMembers(
                groupId, GroupMemberStatus.ACTIVE);

        // Filter out inactive user accounts and exclude leader from members list
        List<GroupMemberResponse> memberResponses = allMembers.stream()
                .filter(member -> Boolean.TRUE.equals(member.getUser().getUserIsActive())) // Only active user accounts
                .filter(member -> !member.getUser().getUserId().equals(leader.getUser().getUserId()))
                .map(this::mapToGroupMemberResponse)
                .collect(Collectors.toList());

        // Get total member count (only active users)
        Long totalActiveUserCount = allMembers.stream()
                .filter(member -> Boolean.TRUE.equals(member.getUser().getUserIsActive()))
                .count();

        // Get current user's role
        GroupMemberRole currentUserRole = allMembers.stream()
                .filter(member -> member.getUser().getUserId().equals(currentUser.getUserId()))
                .map(GroupMember::getRole)
                .findFirst()
                .orElse(GroupMemberRole.MEMBER);

        return GroupMemberListResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .groupAvatarUrl(group.getGroupAvatarUrl())
                .totalMembersCount(totalActiveUserCount.intValue())
                .groupLeader(leaderResponse)
                .members(memberResponses)
                .currentUserRole(currentUserRole)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PendingGroupMemberListResponse getPendingGroupMembers(Integer groupId) {
        log.info("Getting pending member requests for group ID: {}", groupId);

        // Validate input
        if (groupId == null) {
            throw new GroupException("Group ID cannot be null");
        }

        // Get current user
        User currentUser = getCurrentUser();

        // Find and validate group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException("Group not found"));

        // Check if group is active
        if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
            throw new GroupException("Group not found");
        }

        // Get current user's membership and role
        GroupMember currentMember = groupMemberRepository.findByUserAndGroupAndStatus(
                        currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE)
                .orElseThrow(() -> new GroupException("You are not a member of this group"));

        // Check if current user has permission to view pending requests (only OWNER and ADMIN)
        if (!groupPermissionService.canViewPendingRequests(currentMember.getRole())) {
            throw new GroupException("You don't have permission to view pending member requests. Only admins and owners can view pending requests.");
        }

        // Get all pending members
        List<GroupMember> pendingMembers = groupMemberRepository.findPendingGroupMembers(
                groupId, GroupMemberStatus.PENDING_APPROVAL);

        // Map to response DTOs
        List<PendingGroupMemberResponse> pendingResponses = pendingMembers.stream()
                .map(this::mapToPendingGroupMemberResponse)
                .collect(Collectors.toList());

        // Get total pending count
        Long totalPendingCount = groupMemberRepository.countPendingGroupMembers(
                groupId, GroupMemberStatus.PENDING_APPROVAL);

        return PendingGroupMemberListResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .totalPendingCount(totalPendingCount.intValue())
                .pendingMembers(pendingResponses)
                .build();
    }

    @Override
    @Transactional
    public void acceptGroupMemberRequest(Integer groupId, AcceptGroupMemberRequest request) {
        // Validate input first
        validateAcceptRequestInput(groupId, request);
        
        log.info("Accepting group member request for group ID: {}, user ID: {}", groupId, request.getUserId());

        // Get current user
        User currentUser = getCurrentUser();

        // Find and validate group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException("Group not found"));

        // Check if group is active
        if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
            throw new GroupException("Group not found");
        }

        // Get current user's membership and validate permission
        GroupMember currentMember = groupMemberRepository.findByUserAndGroupAndStatus(
                        currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE)
                .orElseThrow(() -> new GroupException("You are not a member of this group"));

        // Check permission
        if (!groupPermissionService.canAcceptGroupMemberRequests(currentMember.getRole())) {
            throw new GroupException("You don't have permission to accept member requests. Only admins and owners can accept requests.");
        }

        // Find pending member request
        GroupMember pendingMember = groupMemberRepository.findByUserAndGroupAndStatus(
                        request.getUserId(), groupId, GroupMemberStatus.PENDING_APPROVAL)
                .orElseThrow(() -> new GroupException("No pending request found for this user"));

        // Validate user account is still active
        if (!Boolean.TRUE.equals(pendingMember.getUser().getUserIsActive())) {
            throw new GroupException("User account is no longer active");
        }

        // Validate business rules before accepting
        groupValidationService.validateUserCanJoinGroup(request.getUserId(), groupId);

        // Accept the request: change status to ACTIVE
        pendingMember.setStatus(GroupMemberStatus.ACTIVE);
        pendingMember.setRole(GroupMemberRole.MEMBER); // Ensure role is set to MEMBER
        groupMemberRepository.save(pendingMember);

        // Remove invitation token from Redis after successful acceptance
        try {
            boolean tokenRemoved = invitationTokenService.removeInvitationTokenByUserAndGroup(
                    request.getUserId(), groupId);
            log.info("Invitation token removal for user {} in group {}: {}", 
                    request.getUserId(), groupId, tokenRemoved ? "success" : "not found");
        } catch (Exception e) {
            log.warn("Failed to remove invitation token for user {} in group {} - continuing with acceptance", 
                    request.getUserId(), groupId, e);
        }

        // Send email to the accepted user
        try {
            sendAcceptanceEmail(pendingMember.getUser(), group, currentUser);
        } catch (Exception e) {
            log.error("Failed to send acceptance email to user {} for group {}", 
                    request.getUserId(), groupId, e);
            // Don't fail the entire acceptance process if email fails
        }

        log.info("Successfully accepted group member request for user {} in group {}", 
                request.getUserId(), groupId);
    }

    @Override
    @Transactional
    public void rejectGroupMemberRequest(Integer groupId, RejectGroupMemberRequest request) {
        // Validate input first
        validateRejectRequestInput(groupId, request);
        
        log.info("Rejecting group member request for group ID: {}, user ID: {}", groupId, request.getUserId());

        // Get current user
        User currentUser = getCurrentUser();

        // Find and validate group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException("Group not found"));

        // Check if group is active
        if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
            throw new GroupException("Group not found");
        }

        // Get current user's membership and validate permission
        GroupMember currentMember = groupMemberRepository.findByUserAndGroupAndStatus(
                        currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE)
                .orElseThrow(() -> new GroupException("You are not a member of this group"));

        // Check permission
        if (!groupPermissionService.canRejectGroupMemberRequests(currentMember.getRole())) {
            throw new GroupException("You don't have permission to reject member requests. Only admins and owners can reject requests.");
        }

        // Find pending member request
        GroupMember pendingMember = groupMemberRepository.findByUserAndGroupAndStatus(
                        request.getUserId(), groupId, GroupMemberStatus.PENDING_APPROVAL)
                .orElseThrow(() -> new GroupException("No pending request found for this user"));

        // Get the rejected user
        User rejectedUser = pendingMember.getUser();

        // Reject the request: change status to REJECTED
        pendingMember.setStatus(GroupMemberStatus.REJECTED);
        groupMemberRepository.save(pendingMember);

        // Remove invitation token from Redis after successful rejection
        try {
            boolean tokenRemoved = invitationTokenService.removeInvitationTokenByUserAndGroup(
                    request.getUserId(), groupId);
            log.info("Invitation token removal for user {} in group {}: {}", 
                    request.getUserId(), groupId, tokenRemoved ? "success" : "not found");
        } catch (Exception e) {
            log.warn("Failed to remove invitation token for user {} in group {} - continuing with rejection", 
                    request.getUserId(), groupId, e);
        }

        // Send rejection email to the rejected user
        try {
            sendRejectionEmail(rejectedUser, group, currentUser);
        } catch (Exception e) {
            log.error("Failed to send rejection email to user {} for group {}", 
                    request.getUserId(), groupId, e);
            // Don't fail the entire rejection process if email fails
        }

        log.info("Successfully rejected group member request for user {} in group {}", 
                request.getUserId(), groupId);
    }

    /**
     * Validate accept request input parameters
     */
    private void validateAcceptRequestInput(Integer groupId, AcceptGroupMemberRequest request) {
        if (groupId == null) {
            throw new GroupException("Group ID cannot be null");
        }
        if (request == null) {
            throw new GroupException("Request cannot be null");
        }
        if (request.getUserId() == null) {
            throw new GroupException("User ID cannot be null");
        }
    }

    /**
     * Validate reject request input parameters
     */
    private void validateRejectRequestInput(Integer groupId, RejectGroupMemberRequest request) {
        if (groupId == null) {
            throw new GroupException("Group ID cannot be null");
        }
        if (request == null) {
            throw new GroupException("Request cannot be null");
        }
        if (request.getUserId() == null) {
            throw new GroupException("User ID cannot be null");
        }
    }

    /**
     * Map GroupMember entity to GroupMemberResponse DTO
     */
    private GroupMemberResponse mapToGroupMemberResponse(GroupMember groupMember) {
        User user = groupMember.getUser();
        GroupMemberResponse response = new GroupMemberResponse();
        response.setUserId(user.getUserId());
        response.setUserFullName(user.getUserFullName());
        response.setUserAvatarUrl(user.getUserAvatarUrl());
        response.setRole(groupMember.getRole());
        return response;
    }

    /**
     * Map GroupMember entity to PendingGroupMemberResponse DTO
     */
    private PendingGroupMemberResponse mapToPendingGroupMemberResponse(GroupMember groupMember) {
        User user = groupMember.getUser();
        PendingGroupMemberResponse response = new PendingGroupMemberResponse();
        response.setUserId(user.getUserId());
        response.setUserFullName(user.getUserFullName());
        response.setUserAvatarUrl(user.getUserAvatarUrl());
        response.setUserEmail(user.getUserEmail());
        response.setRequestedAt(groupMember.getJoinDate());
        return response;
    }

    /**
     * Get current user with validation
     */
    private User getCurrentUser() {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new GroupException("Unable to identify the current user");
        }
        return currentUser;
    }

    @Override
    @Transactional
    public void handleUserAccountDeactivation(Integer userId) {
        log.info("Handling account deactivation for user ID: {}", userId);

        if (userId == null) {
            throw new GroupException("User ID cannot be null");
        }

        // Find all groups where this user has leadership roles (ADMIN or OWNER)
        List<GroupMember> adminRoles = groupMemberRepository.findByUserIdAndRole(userId, GroupMemberRole.ADMIN);
        List<GroupMember> ownerRoles = groupMemberRepository.findByUserIdAndRole(userId, GroupMemberRole.OWNER);

        // Handle OWNER role deactivation first (higher priority)
        for (GroupMember ownerRole : ownerRoles) {
            Group group = ownerRole.getGroup();
            
            // Skip if group is already inactive
            if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
                continue;
            }

            log.info("Processing OWNER account deactivation for group ID: {} (user {} was owner)", 
                    group.getGroupId(), userId);

            // Find active admins to promote to owner
            List<GroupMember> activeAdmins = groupMemberRepository.findActiveGroupMembers(
                    group.getGroupId(), GroupMemberStatus.ACTIVE)
                    .stream()
                    .filter(member -> member.getRole() == GroupMemberRole.ADMIN)
                    .filter(member -> Boolean.TRUE.equals(member.getUser().getUserIsActive()))
                    .collect(Collectors.toList());

            if (!activeAdmins.isEmpty()) {
                // Promote the first active admin to owner
                GroupMember newOwner = activeAdmins.get(0);
                newOwner.setRole(GroupMemberRole.OWNER);
                groupMemberRepository.save(newOwner);
                
                log.info("Promoted user {} from ADMIN to OWNER for group {}", 
                        newOwner.getUser().getUserId(), group.getGroupId());
                continue;
            }

            // No active admins available, find active members to promote
            List<GroupMember> activeMembers = groupMemberRepository.findActiveGroupMembers(
                    group.getGroupId(), GroupMemberStatus.ACTIVE)
                    .stream()
                    .filter(member -> member.getRole() == GroupMemberRole.MEMBER)
                    .filter(member -> Boolean.TRUE.equals(member.getUser().getUserIsActive()))
                    .collect(Collectors.toList());

            if (!activeMembers.isEmpty()) {
                // Promote the first active member to owner
                GroupMember newOwner = activeMembers.get(0);
                newOwner.setRole(GroupMemberRole.OWNER);
                groupMemberRepository.save(newOwner);
                
                log.info("Promoted user {} from MEMBER to OWNER for group {}", 
                        newOwner.getUser().getUserId(), group.getGroupId());
            } else {
                // No active members left, deactivate the group
                group.setGroupIsActive(false);
                groupRepository.save(group);
                
                log.info("No active members left in group {}, group has been deactivated", group.getGroupId());
            }
        }

        // Handle ADMIN role deactivation
        for (GroupMember adminRole : adminRoles) {
            Group group = adminRole.getGroup();
            
            // Skip if group is already inactive
            if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
                continue;
            }

            log.info("Processing ADMIN account deactivation for group ID: {} (user {} was admin)", 
                    group.getGroupId(), userId);

            // Check if there's still an active owner
            Optional<GroupMember> activeOwner = groupMemberRepository.findGroupOwner(
                    group.getGroupId(), GroupMemberStatus.ACTIVE);
            
            if (activeOwner.isPresent() && Boolean.TRUE.equals(activeOwner.get().getUser().getUserIsActive())) {
                // Owner exists and is active, no action needed for admin removal
                log.info("Group {} has active owner, no action needed for admin removal", group.getGroupId());
                continue;
            }

            // Find other active admins in the group
            List<GroupMember> otherActiveAdmins = groupMemberRepository.findActiveGroupMembers(
                    group.getGroupId(), GroupMemberStatus.ACTIVE)
                    .stream()
                    .filter(member -> member.getRole() == GroupMemberRole.ADMIN)
                    .filter(member -> !member.getUser().getUserId().equals(userId))
                    .filter(member -> Boolean.TRUE.equals(member.getUser().getUserIsActive()))
                    .collect(Collectors.toList());

            if (!otherActiveAdmins.isEmpty()) {
                // There are other active admins, no action needed
                log.info("Group {} has other active admins, no leadership transfer needed", group.getGroupId());
                continue;
            }

            // Find active members to promote
            List<GroupMember> activeMembers = groupMemberRepository.findActiveGroupMembers(
                    group.getGroupId(), GroupMemberStatus.ACTIVE)
                    .stream()
                    .filter(member -> member.getRole() == GroupMemberRole.MEMBER)
                    .filter(member -> Boolean.TRUE.equals(member.getUser().getUserIsActive()))
                    .collect(Collectors.toList());

            if (!activeMembers.isEmpty()) {
                // Promote the first active member to admin
                GroupMember newAdmin = activeMembers.get(0);
                newAdmin.setRole(GroupMemberRole.ADMIN);
                groupMemberRepository.save(newAdmin);
                
                log.info("Promoted user {} to admin for group {}", 
                        newAdmin.getUser().getUserId(), group.getGroupId());
            } else {
                // No active members left, deactivate the group
                group.setGroupIsActive(false);
                groupRepository.save(group);
                
                log.info("No active members left in group {}, group has been deactivated", group.getGroupId());
            }
        }

        log.info("Account deactivation handling completed for user ID: {}", userId);
    }

    @Override
    @Transactional
    public void removeMemberFromGroup(Integer groupId, Integer memberUserId) {
        log.info("Removing member {} from group {}", memberUserId, groupId);

        // Validate input parameters
        validateRemoveMemberInput(groupId, memberUserId);

        // Get current user (the one performing the removal)
        User currentUser = getCurrentUser();

        // Validate group and user's permission
        Group group = validateGroupAndPermission(groupId, currentUser);

        // Find the member to be removed
        GroupMember memberToRemove = findActiveMemberToRemove(groupId, memberUserId);

        // Validate business rules for removal with hierarchy
        validateRemovalBusinessRulesWithHierarchy(groupId, currentUser, memberToRemove);

        // Remove the member by setting status to LEFT
        memberToRemove.setStatus(GroupMemberStatus.LEFT);
        groupMemberRepository.save(memberToRemove);

        // Gửi email cho user bị xóa
        try {
            sendMemberRemovedEmailToRemovedUser(group, memberToRemove.getUser(), currentUser);
        } catch (Exception e) {
            log.error("Failed to send member removed email to user {} for group {}", memberUserId, groupId, e);
        }

        // Gửi email cho các thành viên còn lại
        try {
            sendMemberRemovedEmailToGroup(group, memberToRemove.getUser(), currentUser);
        } catch (Exception e) {
            log.error("Failed to send member removed email to group {} for user {}", groupId, memberUserId, e);
        }

        // Gửi notification cho cả nhóm (trừ user bị xóa)
        try {
            notificationService.sendMemberRemovedNotificationToGroup(
                groupId,
                memberUserId,
                memberToRemove.getUser().getUserFullName(),
                currentUser.getUserFullName()
            );
        } catch (Exception e) {
            log.error("Failed to send member removed notification to group {} for user {}", groupId, memberUserId, e);
        }

        log.info("Successfully removed member {} from group {}", memberUserId, groupId);
    }

    /**
     * Gửi email cho user bị xóa khỏi group
     */
    private void sendMemberRemovedEmailToRemovedUser(Group group, User removedUser, User removedByUser) {
        Context context = new Context();
        context.setVariable("removedUserName", removedUser.getUserFullName());
        context.setVariable("removedByUserName", removedByUser.getUserFullName());
        context.setVariable("groupName", group.getGroupName());
        context.setVariable("groupAvatarUrl", group.getGroupAvatarUrl());
        context.setVariable("appName", appProperties.getLabName());
        String subject = String.format("You have been removed from '%s' group on %s", group.getGroupName(), appProperties.getLabName());
        emailService.sendEmailWithHtmlTemplate(
            removedUser.getUserEmail(),
            subject,
            "group-member-removed",
            context
        );
    }

    /**
     * Gửi email cho các thành viên còn lại khi có thành viên bị xóa
     */
    private void sendMemberRemovedEmailToGroup(Group group, User removedUser, User removedByUser) {
        List<GroupMember> groupMembers = groupMemberRepository.findByGroupAndStatusAndUserIdNot(
            group.getGroupId(), GroupMemberStatus.ACTIVE, removedUser.getUserId()
        );
        if (groupMembers.isEmpty()) return;
        Context context = new Context();
        context.setVariable("removedUserName", removedUser.getUserFullName());
        context.setVariable("removedByUserName", removedByUser.getUserFullName());
        context.setVariable("groupName", group.getGroupName());
        context.setVariable("groupAvatarUrl", group.getGroupAvatarUrl());
        context.setVariable("appName", appProperties.getLabName());
        String subject = String.format("Member removed from '%s' group on %s", group.getGroupName(), appProperties.getLabName());
        for (GroupMember member : groupMembers) {
            try {
                emailService.sendEmailWithHtmlTemplate(
                    member.getUser().getUserEmail(),
                    subject,
                    "group-member-removed-notification",
                    context
                );
            } catch (Exception e) {
                log.error("Failed to send member removed notification email to: {} for group: {}", member.getUser().getUserEmail(), group.getGroupId(), e);
            }
        }
    }

    @Override
    @Transactional
    public void updateMemberRole(Integer groupId, Integer memberUserId, UpdateMemberRoleRequest request) {
        log.info("Updating member {} role to {} in group {}", memberUserId, 
                request != null ? request.getNewRole() : "null", groupId);

        // Validate input parameters
        validateUpdateRoleInput(groupId, memberUserId, request);

        Integer currentUserId = getCurrentUser().getUserId();

        // Validate group
        Group group = validateGroupForRoleUpdate(groupId);

        // Validate owner permission
        GroupMember currentUserMember = validateOwnerPermission(currentUserId, group);

        // Find target member
        GroupMember targetMember = findActiveMemberForRoleUpdate(memberUserId, group);

        // Store previous role for email notification
        GroupMemberRole previousRole = targetMember.getRole();

        // Validate business rules
        validateRoleUpdateBusinessRules(currentUserId, memberUserId, targetMember.getRole(), request.getNewRole());

        // Update role
        targetMember.setRole(request.getNewRole());
        groupMemberRepository.save(targetMember);

        // Send role update email to the updated member
        try {
            sendRoleUpdateEmail(targetMember.getUser(), group, currentUserMember.getUser(), previousRole, request.getNewRole());
        } catch (Exception e) {
            log.error("Failed to send role update email to user {} for group {}", 
                    memberUserId, groupId, e);
            // Don't fail the entire update process if email fails
        }

        // Send notification to the updated user
        try {
            notificationService.sendRoleUpdateNotificationToUser(
                groupId,
                memberUserId,
                group.getGroupName(),
                currentUserMember.getUser().getUserFullName(),
                previousRole,
                request.getNewRole()
            );
        } catch (Exception e) {
            log.error("Failed to send role update notification to user {} for group {}", 
                    groupId, memberUserId, e);
            // Don't fail the entire update process if notification fails
        }

        // Send notification to the group about role update
        try {
            notificationService.sendRoleUpdateNotificationToGroup(
                groupId,
                memberUserId,
                targetMember.getUser().getUserFullName(),
                currentUserMember.getUser().getUserFullName(),
                previousRole,
                request.getNewRole()
            );
        } catch (Exception e) {
            log.error("Failed to send role update notification to group {} for user {}", 
                    groupId, memberUserId, e);
            // Don't fail the entire update process if notification fails
        }

        // Send role update email to all group members
        try {
            sendRoleUpdateEmailToGroup(group, targetMember.getUser(), currentUserMember.getUser(), previousRole, request.getNewRole());
        } catch (Exception e) {
            log.error("Failed to send role update email to group {} for user {}", 
                    groupId, memberUserId, e);
            // Don't fail the entire update process if email fails
        }

        log.info("Successfully updated member {} role from {} to {} in group {}", 
                memberUserId, previousRole, request.getNewRole(), groupId);
    }

    /**
     * Validate input for role update
     */
    private void validateUpdateRoleInput(Integer groupId, Integer memberUserId, UpdateMemberRoleRequest request) {
        if (groupId == null) {
            throw new GroupException("Group ID is required for updating member role");
        }

        if (memberUserId == null) {
            throw new GroupException("Member user ID is required for updating member role");
        }

        if (request == null) {
            throw new GroupException("New role is required");
        }

        if (request.getNewRole() == null) {
            throw new GroupException("New role is required");
        }
    }

    /**
     * Validate group exists and is active for role update
     */
    private Group validateGroupForRoleUpdate(Integer groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException("Group not found"));

        if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
            throw new GroupException("Group not found");
        }

        return group;
    }

    /**
     * Validate current user is owner of the group
     */
    private GroupMember validateOwnerPermission(Integer currentUserId, Group group) {
        Optional<GroupMember> currentUserMembership = groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                currentUserId, group.getGroupId());

        if (currentUserMembership.isEmpty() || 
            currentUserMembership.get().getStatus() != GroupMemberStatus.ACTIVE) {
            throw new GroupException("You are not an active member of this group");
        }

        GroupMember currentUserMember = currentUserMembership.get();
        GroupMemberRole currentUserRole = currentUserMember.getRole();
        if (currentUserRole != GroupMemberRole.OWNER) {
            throw new GroupException("Only group owner can update member roles");
        }

        return currentUserMember;
    }

    /**
     * Find active member for role update
     */
    private GroupMember findActiveMemberForRoleUpdate(Integer memberUserId, Group group) {
        Optional<GroupMember> memberOptional = groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(memberUserId, group.getGroupId());

        if (memberOptional.isEmpty()) {
            throw new GroupException("Member not found in this group");
        }

        GroupMember member = memberOptional.get();

        if (member.getStatus() != GroupMemberStatus.ACTIVE) {
            throw new GroupException("Member is not currently active in this group");
        }

        if (!Boolean.TRUE.equals(member.getUser().getUserIsActive())) {
            throw new GroupException("Cannot update role of inactive user account");
        }

        return member;
    }

    /**
     * Validate business rules for role update
     */
    private void validateRoleUpdateBusinessRules(Integer currentUserId, Integer memberUserId, 
                                                GroupMemberRole currentRole, GroupMemberRole newRole) {
        
        // Cannot update your own role
        if (currentUserId.equals(memberUserId)) {
            throw new GroupException("Cannot update your own role");
        }

        // Check if user already has this role
        if (currentRole == newRole) {
            throw new GroupException("Member already has this role");
        }

        // Use permission service to validate role changes
        if (!groupPermissionService.canUpdateMemberRole(GroupMemberRole.OWNER, currentRole, newRole)) {
            if (currentRole == GroupMemberRole.OWNER) {
                throw new GroupException("Cannot change owner role");
            } else if (newRole == GroupMemberRole.OWNER) {
                throw new GroupException("Cannot promote to owner (prevent multiple owners)");
            } else {
                throw new GroupException("Invalid role update operation");
            }
        }
    }

    /**
     * Validate input parameters for member removal
     */
    private void validateRemoveMemberInput(Integer groupId, Integer memberUserId) {
        if (groupId == null) {
            throw new GroupException("Group ID cannot be null");
        }
        if (memberUserId == null) {
            throw new GroupException("Member user ID cannot be null");
        }
    }

    /**
     * Validate group exists, is active, and current user has permission to remove members
     */
    private Group validateGroupAndPermission(Integer groupId, User currentUser) {
        // Find and validate group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException("Group not found"));

        // Check if group is active
        if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
            throw new GroupException("Group not found");
        }

        // Check if current user is admin or owner of the group
        Optional<GroupMember> currentUserMembership = groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                currentUser.getUserId(), groupId);
        
        if (currentUserMembership.isEmpty() || 
            currentUserMembership.get().getStatus() != GroupMemberStatus.ACTIVE ||
            (currentUserMembership.get().getRole() != GroupMemberRole.ADMIN && 
             currentUserMembership.get().getRole() != GroupMemberRole.OWNER)) {
            throw new GroupException("Only group administrators and owners can remove members");
        }

        return group;
    }

    /**
     * Find the active member to be removed
     */
    private GroupMember findActiveMemberToRemove(Integer groupId, Integer memberUserId) {
        // Find the member to be removed
        Optional<GroupMember> memberOptional = groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(memberUserId, groupId);
        
        if (memberOptional.isEmpty()) {
            throw new GroupException("Member not found in this group");
        }

        GroupMember member = memberOptional.get();

        // Check if member is currently active
        if (member.getStatus() != GroupMemberStatus.ACTIVE) {
            throw new GroupException("Member is not currently active in this group");
        }

        // Check if the user account is still active
        if (!Boolean.TRUE.equals(member.getUser().getUserIsActive())) {
            throw new GroupException("Cannot remove inactive user account");
        }

        return member;
    }

    /**
     * Validate business rules for member removal with proper hierarchy
     * Hierarchy: OWNER > ADMIN > MEMBER
     * Rules:
     * 1. OWNER cannot be removed by anyone (including themselves)
     * 2. Only OWNER can remove ADMIN
     * 3. ADMIN can remove MEMBER
     * 4. OWNER can remove MEMBER
     * 5. Cannot remove last ADMIN if no OWNER exists (edge case protection)
     */
    private void validateRemovalBusinessRulesWithHierarchy(Integer groupId, User currentUser, GroupMember memberToRemove) {
        Integer memberUserId = memberToRemove.getUser().getUserId();
        GroupMemberRole memberRole = memberToRemove.getRole();
        
        // Get current user's role
        GroupMemberRole currentUserRole = groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(
                currentUser.getUserId(), groupId)
                .map(GroupMember::getRole)
                .orElseThrow(() -> new GroupException("Current user membership not found"));

        log.info("Permission check: {} (role: {}) attempting to remove {} (role: {})", 
                currentUser.getUserId(), currentUserRole, memberUserId, memberRole);

        // Rule 1: OWNER cannot be removed by anyone (including themselves)
        if (memberRole == GroupMemberRole.OWNER) {
            throw new GroupException("Group owner cannot be removed from the group");
        }

        // Rule 2-3: Use permission service for role-based removal checks
        if (!groupPermissionService.canRemoveMember(currentUserRole, memberRole)) {
            String permissionDesc = groupPermissionService.getPermissionDescription(
                "REMOVE_MEMBER", currentUserRole, memberRole);
            log.warn("Permission denied: {}", permissionDesc);
            
            throw new GroupException("Insufficient permission to remove this member");
        }

        // Rule 4: Special case for removing admin - check if it's the last admin without owner
        if (memberRole == GroupMemberRole.ADMIN) {
            List<GroupMember> activeAdmins = groupMemberRepository.findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE)
                    .stream()
                    .filter(member -> member.getRole() == GroupMemberRole.ADMIN)
                    .filter(member -> Boolean.TRUE.equals(member.getUser().getUserIsActive()))
                    .collect(Collectors.toList());

            // Check if there's an owner
            boolean hasOwner = groupMemberRepository.findActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE)
                    .stream()
                    .anyMatch(member -> member.getRole() == GroupMemberRole.OWNER && 
                              Boolean.TRUE.equals(member.getUser().getUserIsActive()));

            // Use permission service for context-aware last admin check
            if (!groupPermissionService.canRemoveLastAdmin(currentUserRole, hasOwner, activeAdmins.size())) {
                log.warn("Cannot remove last admin - hasOwner: {}, adminCount: {}", hasOwner, activeAdmins.size());
                throw new GroupException("Cannot remove the last administrator. Please promote another member to admin first");
            }
        }
    }

    @Override
    @Transactional
    public void exitGroup(Integer groupId) {
        log.info("User attempting to exit group ID: {}", groupId);

        // Validate input
        validateExitGroupInput(groupId);

        // Get current user
        User currentUser = getCurrentUser();

        // Validate group and find member
        Group group = validateGroupForExit(groupId);
        GroupMember currentMember = findActiveMemberForExit(currentUser.getUserId(), group.getGroupId());

        // For OWNER, we need special handling - return error with specific message for frontend to handle
        if (currentMember.getRole() == GroupMemberRole.OWNER) {
            throw new GroupException("As group owner, you must transfer ownership or delete the group before leaving.");
        }

        // For ADMIN and MEMBER, proceed with normal exit
        currentMember.setStatus(GroupMemberStatus.LEFT);
        groupMemberRepository.save(currentMember);

        log.info("User {} successfully exited group {}", currentUser.getUserId(), groupId);
    }

    @Override
    @Transactional
    public void transferOwnership(Integer groupId, TransferOwnershipRequest request) {
        log.info("Transferring ownership for group ID: {} to user ID: {}", groupId, request.getNewOwnerUserId());

        // Validate input
        validateTransferOwnershipInput(groupId, request);

        // Get current user
        User currentUser = getCurrentUser();

        // Validate group and current user's ownership
        Group group = validateGroupForOwnershipTransfer(groupId);
        GroupMember currentOwner = validateCurrentUserIsOwner(currentUser.getUserId(), groupId);

        // Find and validate new owner
        GroupMember newOwner = validateNewOwnerEligibility(request.getNewOwnerUserId(), groupId);

        // Perform ownership transfer
        currentOwner.setRole(GroupMemberRole.MEMBER); // Demote current owner to member
        newOwner.setRole(GroupMemberRole.OWNER); // Promote new member to owner

        // Save changes
        groupMemberRepository.save(currentOwner);
        groupMemberRepository.save(newOwner);

        log.info("Successfully transferred ownership of group {} from user {} to user {}", 
                groupId, currentUser.getUserId(), request.getNewOwnerUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public GroupMemberListResponse getEligibleMembersForOwnership(Integer groupId) {
        log.info("Getting eligible members for ownership transfer in group ID: {}", groupId);

        // Validate input
        if (groupId == null) {
            throw new GroupException("Group ID cannot be null");
        }

        // Get current user
        User currentUser = getCurrentUser();

        // Validate group and current user's ownership
        Group group = validateGroupForOwnershipTransfer(groupId);
        validateCurrentUserIsOwner(currentUser.getUserId(), groupId);

        // Get all active members except current owner
        List<GroupMember> eligibleMembers = groupMemberRepository.findActiveGroupMembers(
                groupId, GroupMemberStatus.ACTIVE)
                .stream()
                .filter(member -> !member.getUser().getUserId().equals(currentUser.getUserId())) // Exclude current owner
                .filter(member -> Boolean.TRUE.equals(member.getUser().getUserIsActive())) // Only active user accounts
                .collect(Collectors.toList());

        if (eligibleMembers.isEmpty()) {
            throw new GroupException("No eligible members found for ownership transfer. Group must have at least one other active member.");
        }

        // Convert to response format
        List<GroupMemberResponse> memberResponses = eligibleMembers.stream()
                .map(this::mapToGroupMemberResponse)
                .collect(Collectors.toList());

        GroupMemberListResponse response = new GroupMemberListResponse();
        response.setGroupId(group.getGroupId());
        response.setGroupName(group.getGroupName());
        response.setGroupAvatarUrl(group.getGroupAvatarUrl());
        response.setTotalMembersCount(memberResponses.size());
        response.setMembers(memberResponses);
        response.setCurrentUserRole(GroupMemberRole.OWNER);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerExitOptionsResponse getOwnerExitOptions(Integer groupId) {
        log.info("Getting owner exit options for group ID: {}", groupId);

        // Validate input
        if (groupId == null) {
            throw new GroupException("Group ID cannot be null");
        }

        // Get current user
        User currentUser = getCurrentUser();

        // Validate group and current user's ownership
        Group group = validateGroupForOwnershipTransfer(groupId);
        validateCurrentUserIsOwner(currentUser.getUserId(), groupId);

        // Count eligible members for ownership transfer
        List<GroupMember> eligibleMembers = groupMemberRepository.findActiveGroupMembers(
                groupId, GroupMemberStatus.ACTIVE)
                .stream()
                .filter(member -> !member.getUser().getUserId().equals(currentUser.getUserId())) // Exclude current owner
                .filter(member -> Boolean.TRUE.equals(member.getUser().getUserIsActive())) // Only active user accounts
                .collect(Collectors.toList());

        int eligibleMembersCount = eligibleMembers.size();
        boolean canTransferOwnership = eligibleMembersCount > 0;
        boolean canDeleteGroup = true; // Owner can always delete group

        String message;
        if (canTransferOwnership) {
            message = String.format("You have %d eligible member(s) to transfer ownership to, or you can delete the group.", 
                    eligibleMembersCount);
        } else {
            message = "No other members available for ownership transfer. You can only delete the group.";
        }

        return OwnerExitOptionsResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .canTransferOwnership(canTransferOwnership)
                .canDeleteGroup(canDeleteGroup)
                .eligibleMembersCount(eligibleMembersCount)
                .message(message)
                .build();
    }

    // Helper methods for ownership transfer validation

    private void validateTransferOwnershipInput(Integer groupId, TransferOwnershipRequest request) {
        if (groupId == null) {
            throw new GroupException("Group ID cannot be null");
        }
        if (request == null) {
            throw new GroupException("Transfer ownership request cannot be null");
        }
        if (request.getNewOwnerUserId() == null) {
            throw new GroupException("New owner user ID cannot be null");
        }
    }

    private Group validateGroupForOwnershipTransfer(Integer groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException("Group not found"));

        if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
            throw new GroupException("Cannot transfer ownership of inactive group");
        }

        return group;
    }

    private GroupMember validateCurrentUserIsOwner(Integer userId, Integer groupId) {
        Optional<GroupMember> memberOptional = groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(userId, groupId);

        if (memberOptional.isEmpty()) {
            throw new GroupException("You are not a member of this group");
        }

        GroupMember member = memberOptional.get();

        if (member.getStatus() != GroupMemberStatus.ACTIVE) {
            throw new GroupException("You are not currently active in this group");
        }

        if (member.getRole() != GroupMemberRole.OWNER) {
            throw new GroupException("Only group owner can transfer ownership");
        }

        return member;
    }

    private GroupMember validateNewOwnerEligibility(Integer newOwnerUserId, Integer groupId) {
        Optional<GroupMember> memberOptional = groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(newOwnerUserId, groupId);

        if (memberOptional.isEmpty()) {
            throw new GroupException("Selected user is not a member of this group");
        }

        GroupMember member = memberOptional.get();

        if (member.getStatus() != GroupMemberStatus.ACTIVE) {
            throw new GroupException("Selected user is not currently active in this group");
        }

        if (!Boolean.TRUE.equals(member.getUser().getUserIsActive())) {
            throw new GroupException("Selected user account is not active");
        }

        return member;
    }

    /**
     * Validate input for exit group operation
     */
    private void validateExitGroupInput(Integer groupId) {
        if (groupId == null) {
            throw new GroupException("Group ID cannot be null");
        }
    }

    /**
     * Validate group exists and is active for exit operation
     */
    private Group validateGroupForExit(Integer groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException("Group not found"));

        if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
            throw new GroupException("Group not found");
        }

        return group;
    }

    /**
     * Find active member for exit operation
     */
    private GroupMember findActiveMemberForExit(Integer userId, Integer groupId) {
        Optional<GroupMember> memberOptional = groupMemberRepository.findMostRecentMembershipByUserIdAndGroupId(userId, groupId);

        if (memberOptional.isEmpty()) {
            throw new GroupException("You are not a member of this group");
        }

        GroupMember member = memberOptional.get();

        if (member.getStatus() != GroupMemberStatus.ACTIVE) {
            throw new GroupException("You are not currently active in this group");
        }

        return member;
    }

    /**
     * Validate exit permissions - OWNER cannot exit, ADMIN and MEMBER can exit
     */
    private void validateExitPermissions(GroupMember currentMember) {
        GroupMemberRole role = currentMember.getRole();
        
        if (role == GroupMemberRole.OWNER) {
            throw new GroupException("Group owner cannot exit the group. Please transfer ownership before leaving.");
        }
        
        // ADMIN and MEMBER can exit
        if (role != GroupMemberRole.ADMIN && role != GroupMemberRole.MEMBER) {
            throw new GroupException("Invalid role for exit operation");
        }
    }

    /**
     * Send rejection email to the user
     */
    private void sendRejectionEmail(User rejectedUser, Group group, User reviewedByUser) {
        try {
            // Get member count
            Long memberCount = groupMemberRepository.countActiveGroupMembers(
                    group.getGroupId(), GroupMemberStatus.ACTIVE);

            // Prepare email context
            Context context = new Context();
            context.setVariable("rejectedUserName", rejectedUser.getUserFullName());
            context.setVariable("reviewedByUserName", reviewedByUser.getUserFullName());
            context.setVariable("groupName", group.getGroupName());
            context.setVariable("groupAvatarUrl", group.getGroupAvatarUrl());
            context.setVariable("memberCount", memberCount.intValue());
            context.setVariable("appName", appProperties.getLabName());

            // Send email
            String subject = String.format("Group request update for '%s' on %s", 
                    group.getGroupName(), appProperties.getLabName());
            
            emailService.sendEmailWithHtmlTemplate(
                    rejectedUser.getUserEmail(),
                    subject,
                    groupRejectionHtmlTemplate,
                    context
            );

            log.info("Rejection email sent successfully to: {}", rejectedUser.getUserEmail());
            
        } catch (Exception e) {
            log.error("Failed to send rejection email to: {} for group: {}", 
                    rejectedUser.getUserEmail(), group.getGroupId(), e);
            throw e; // Re-throw to be caught by the calling method
        }
    }

    /**
     * Send acceptance email to the user
     */
    private void sendAcceptanceEmail(User acceptedUser, Group group, User acceptedByUser) {
        try {
            // Get member count
            Long memberCount = groupMemberRepository.countActiveGroupMembers(
                    group.getGroupId(), GroupMemberStatus.ACTIVE);

            // Prepare email context
            Context context = new Context();
            context.setVariable("acceptedUserName", acceptedUser.getUserFullName());
            context.setVariable("acceptedByUserName", acceptedByUser.getUserFullName());
            context.setVariable("groupName", group.getGroupName());
            context.setVariable("groupAvatarUrl", group.getGroupAvatarUrl());
            context.setVariable("memberCount", memberCount.intValue());
            context.setVariable("appName", appProperties.getLabName());

            // Send email
            String subject = String.format("Welcome to '%s' group on %s", 
                    group.getGroupName(), appProperties.getLabName());
            
            emailService.sendEmailWithHtmlTemplate(
                    acceptedUser.getUserEmail(),
                    subject,
                    groupAcceptanceHtmlTemplate,
                    context
            );

            log.info("Acceptance email sent successfully to: {}", acceptedUser.getUserEmail());
            
        } catch (Exception e) {
            log.error("Failed to send acceptance email to: {} for group: {}", 
                    acceptedUser.getUserEmail(), group.getGroupId(), e);
            throw e; // Re-throw to be caught by the calling method
        }
    }

    /**
     * Send role update email to the user
     */
    private void sendRoleUpdateEmail(User updatedUser, Group group, User updatedByUser, 
                                   GroupMemberRole previousRole, GroupMemberRole newRole) {
        try {
            // Get member count
            Long memberCount = groupMemberRepository.countActiveGroupMembers(
                    group.getGroupId(), GroupMemberStatus.ACTIVE);

            // Prepare email context
            Context context = new Context();
            context.setVariable("updatedUserName", updatedUser.getUserFullName());
            context.setVariable("updatedByUserName", updatedByUser.getUserFullName());
            context.setVariable("groupName", group.getGroupName());
            context.setVariable("groupAvatarUrl", group.getGroupAvatarUrl());
            context.setVariable("memberCount", memberCount.intValue());
            context.setVariable("previousRole", previousRole.name());
            context.setVariable("newRole", newRole.name());
            context.setVariable("appName", appProperties.getLabName());

            // Send email
            String subject = String.format("Role update in '%s' group on %s", 
                    group.getGroupName(), appProperties.getLabName());
            
            emailService.sendEmailWithHtmlTemplate(
                    updatedUser.getUserEmail(),
                    subject,
                    groupRoleUpdateHtmlTemplate,
                    context
            );

            log.info("Role update email sent successfully to: {}", updatedUser.getUserEmail());
            
        } catch (Exception e) {
            log.error("Failed to send role update email to: {} for group: {}", 
                    updatedUser.getUserEmail(), group.getGroupId(), e);
            throw e; // Re-throw to be caught by the calling method
        }
    }

    /**
     * Send role update email to all group members
     */
    private void sendRoleUpdateEmailToGroup(Group group, User updatedUser, User updatedByUser, 
                                          GroupMemberRole previousRole, GroupMemberRole newRole) {
        try {
            // Get all active members of the group (excluding the updated user)
            List<GroupMember> groupMembers = groupMemberRepository.findByGroupAndStatusAndUserIdNot(
                    group.getGroupId(), GroupMemberStatus.ACTIVE, updatedUser.getUserId());
            
            if (groupMembers.isEmpty()) {
                log.warn("No active members found in group: {} (excluding updated user: {})", 
                        group.getGroupId(), updatedUser.getUserId());
                return;
            }

            // Get member count
            Long memberCount = groupMemberRepository.countActiveGroupMembers(
                    group.getGroupId(), GroupMemberStatus.ACTIVE);

            // Prepare email context
            Context context = new Context();
            context.setVariable("updatedUserName", updatedUser.getUserFullName());
            context.setVariable("updatedByUserName", updatedByUser.getUserFullName());
            context.setVariable("groupName", group.getGroupName());
            context.setVariable("groupAvatarUrl", group.getGroupAvatarUrl());
            context.setVariable("memberCount", memberCount.intValue());
            context.setVariable("previousRole", previousRole.name());
            context.setVariable("newRole", newRole.name());
            context.setVariable("appName", appProperties.getLabName());

            // Send email to all group members
            String subject = String.format("Group member role update in '%s' group on %s", 
                    group.getGroupName(), appProperties.getLabName());
            
            for (GroupMember member : groupMembers) {
                try {
                    emailService.sendEmailWithHtmlTemplate(
                            member.getUser().getUserEmail(),
                            subject,
                            groupRoleUpdateNotificationHtmlTemplate,
                            context
                    );
                    log.debug("Role update notification email sent successfully to: {}", member.getUser().getUserEmail());
                } catch (Exception e) {
                    log.error("Failed to send role update notification email to: {} for group: {}", 
                            member.getUser().getUserEmail(), group.getGroupId(), e);
                    // Continue with other members even if one fails
                }
            }

            log.info("Role update email sent to {} members in group: {}", groupMembers.size(), group.getGroupId());
            
        } catch (Exception e) {
            log.error("Failed to send role update email to group: {} for user: {}", 
                    group.getGroupId(), updatedUser.getUserId(), e);
            throw e; // Re-throw to be caught by the calling method
        }
    }
} 