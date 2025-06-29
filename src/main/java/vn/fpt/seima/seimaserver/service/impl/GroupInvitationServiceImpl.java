package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.fpt.seima.seimaserver.config.base.AppProperties;
import vn.fpt.seima.seimaserver.dto.request.group.JoinGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberResponse;
import vn.fpt.seima.seimaserver.dto.response.group.InvitationDetailsResponse;
import vn.fpt.seima.seimaserver.dto.response.group.JoinGroupResponse;
import vn.fpt.seima.seimaserver.entity.Group;
import vn.fpt.seima.seimaserver.entity.GroupMember;
import vn.fpt.seima.seimaserver.entity.GroupMemberRole;
import vn.fpt.seima.seimaserver.entity.GroupMemberStatus;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.GroupException;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.repository.GroupRepository;
import vn.fpt.seima.seimaserver.service.GroupInvitationService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementation of GroupInvitationService
 * Follows Single Responsibility Principle and is designed for testability
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupInvitationServiceImpl implements GroupInvitationService {
    
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final AppProperties appProperties;
    
    @Override
    @Transactional(readOnly = true)
    public InvitationDetailsResponse getInvitationDetails(String inviteCode) {
        log.info("Getting invitation details for code: {}", inviteCode);
        
        // Validate input
        validateInviteCode(inviteCode);
        
        // Find group by invite code
        Optional<Group> groupOpt = groupRepository.findByGroupInviteCode(inviteCode);
        
        if (groupOpt.isEmpty()) {
            log.warn("Group not found with invite code: {}", inviteCode);
            return buildInvalidInvitationResponse(inviteCode, "Invitation code not found");
        }
        
        Group group = groupOpt.get();
        
        // Check if group is not active
        if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
            log.warn("Group is not active for invite code: {}", inviteCode);
            return buildInvalidInvitationResponse(inviteCode, "This group is no longer active");
        }
        
        // Get group leader (owner)
        Optional<GroupMember> leaderOpt = groupMemberRepository.findGroupOwner(
            group.getGroupId(), GroupMemberStatus.ACTIVE);
        
        if (leaderOpt.isEmpty()) {
            log.error("No active owner found for group: {}", group.getGroupId());
            return buildInvalidInvitationResponse(inviteCode, "Group configuration error");
        }
        
        // Get member count
        Long memberCount = groupMemberRepository.countActiveGroupMembers(
            group.getGroupId(), GroupMemberStatus.ACTIVE);
        
        // Build successful response
        GroupMember leader = leaderOpt.get();
        GroupMemberResponse leaderResponse = mapToGroupMemberResponse(leader);
        
        // Build invite link
        String inviteLink = buildInviteLink(inviteCode);
        
        InvitationDetailsResponse response = InvitationDetailsResponse.builder()
            .groupId(group.getGroupId())
            .groupName(group.getGroupName())
            .groupAvatarUrl(group.getGroupAvatarUrl())
            .groupCreatedDate(group.getGroupCreatedDate())
            .memberCount(memberCount.intValue())
            .groupLeader(leaderResponse)
            .inviteLink(inviteLink)
            .isValidInvitation(true)
            .message("Invitation is valid")
            .build();
        
        log.info("Successfully retrieved invitation details for group: {} with {} members", 
            group.getGroupName(), memberCount);
        
        return response;
    }
    
    @Override
    @Transactional
    public JoinGroupResponse joinGroupByInviteCode(JoinGroupRequest request) {
        log.info("Processing join group request with invite code: {}", request.getInviteCode());
        
        // Validate request
        validateJoinGroupRequest(request);
        
        // Get current user
        User currentUser = getCurrentUser();
        
        // Find and validate group
        Group group = findAndValidateGroup(request.getInviteCode());
        
        // Check membership and handle reactivation if needed
        GroupMember member = handleUserMembership(currentUser.getUserId(), group);
        
        // Get updated member count
        Long memberCount = groupMemberRepository.countActiveGroupMembers(
            group.getGroupId(), GroupMemberStatus.ACTIVE);
        
        // Build response
        JoinGroupResponse response = JoinGroupResponse.builder()
            .groupId(group.getGroupId())
            .groupName(group.getGroupName())
            .groupAvatarUrl(group.getGroupAvatarUrl())
            .joinedDate(member.getJoinDate())
            .memberCount(memberCount.intValue())
            .message("Successfully joined the group")
            .build();
        
        log.info("User {} successfully joined group {} with invite code: {}", 
            currentUser.getUserId(), group.getGroupId(), request.getInviteCode());
        
        return response;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isInvitationValid(String inviteCode) {
        if (!StringUtils.hasText(inviteCode)) {
            return false;
        }
        
        return groupRepository.existsByGroupInviteCodeAndGroupIsActive(inviteCode, true);
    }
    
    // Private helper methods for better code organization and testability
    
    private void validateInviteCode(String inviteCode) {
        if (!StringUtils.hasText(inviteCode)) {
            throw new GroupException("Invite code cannot be null or empty");
        }
        
        if (inviteCode.trim().length() < 8 || inviteCode.trim().length() > 36) {
            throw new GroupException("Invalid invite code format");
        }
    }
    
    private void validateJoinGroupRequest(JoinGroupRequest request) {
        if (request == null) {
            throw new GroupException("Join group request cannot be null");
        }
        
        validateInviteCode(request.getInviteCode());
    }
    
    private User getCurrentUser() {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new GroupException("Unable to identify the current user");
        }
        return currentUser;
    }
    
    private Group findAndValidateGroup(String inviteCode) {
        Optional<Group> groupOpt = groupRepository.findByGroupInviteCodeAndGroupIsActive(inviteCode, true);
        
        if (groupOpt.isEmpty()) {
            throw new GroupException("Invalid or expired invitation code");
        }
        
        return groupOpt.get();
    }
    
    private GroupMember handleUserMembership(Integer userId, Group group) {
        Integer groupId = group.getGroupId();
        
        // Check if user is already an active member
        if (groupMemberRepository.existsByUserAndGroupAndStatus(userId, groupId, GroupMemberStatus.ACTIVE)) {
            throw new GroupException("You are already a member of this group");
        }
        
        // Check if user has any existing membership (even inactive) and handle appropriately
        Optional<GroupMember> existingMemberOpt = groupMemberRepository.findByUserIdAndGroupId(userId, groupId);
        
        if (existingMemberOpt.isPresent()) {
            GroupMember existingMember = existingMemberOpt.get();
            GroupMemberStatus status = existingMember.getStatus();
            
            switch (status) {
                case PENDING_APPROVAL:
                    throw new GroupException("Your membership request is pending approval");
                case INVITED:
                    // User was invited before, we can reactivate
                    log.info("Reactivating invited user {} for group {}", userId, groupId);
                    existingMember.setStatus(GroupMemberStatus.ACTIVE);
                    existingMember.setJoinDate(LocalDateTime.now());
                    return groupMemberRepository.save(existingMember);
                case REJECTED:
                    throw new GroupException("Your previous request to join this group was rejected");
                case LEFT:
                    // User left before, allow them to rejoin
                    log.info("Allowing user {} to rejoin group {}", userId, groupId);
                    existingMember.setStatus(GroupMemberStatus.ACTIVE);
                    existingMember.setJoinDate(LocalDateTime.now());
                    return groupMemberRepository.save(existingMember);
                default:
                    // This shouldn't happen with current status values
                    log.warn("Unexpected membership status {} for user {} in group {}", status, userId, groupId);
                    throw new GroupException("Unable to process membership due to invalid status");
            }
        }
        
        // Create new membership if no existing membership found
        return createNewGroupMembership(group, userId);
    }
    
    private GroupMember createNewGroupMembership(Group group, Integer userId) {
        // We need to get the User entity - you might need to inject UserRepository
        // For now, assuming UserUtils can provide the current user
        User user = getCurrentUser();
        if (!user.getUserId().equals(userId)) {
            throw new GroupException("User ID mismatch");
        }
        
        GroupMember groupMember = new GroupMember();
        groupMember.setGroup(group);
        groupMember.setUser(user);
        groupMember.setRole(GroupMemberRole.MEMBER);
        groupMember.setStatus(GroupMemberStatus.ACTIVE);
        groupMember.setJoinDate(LocalDateTime.now());
        
        GroupMember savedMember = groupMemberRepository.save(groupMember);
        log.info("Created new group membership for user {} in group {}", 
            user.getUserId(), group.getGroupId());
        
        return savedMember;
    }
    
    private GroupMember createGroupMembership(Group group, User user) {
        GroupMember groupMember = new GroupMember();
        groupMember.setGroup(group);
        groupMember.setUser(user);
        groupMember.setRole(GroupMemberRole.MEMBER);
        groupMember.setStatus(GroupMemberStatus.ACTIVE);
        // joinDate will be set by @CreationTimestamp or we can set it manually
        groupMember.setJoinDate(LocalDateTime.now());
        
        GroupMember savedMember = groupMemberRepository.save(groupMember);
        log.info("Created new group membership for user {} in group {}", 
            user.getUserId(), group.getGroupId());
        
        return savedMember;
    }
    
    private InvitationDetailsResponse buildInvalidInvitationResponse(String inviteCode, String message) {
        String inviteLink = buildInviteLink(inviteCode);
        return InvitationDetailsResponse.builder()
            .inviteLink(inviteLink)
            .isValidInvitation(false)
            .message(message)
            .build();
    }
    
    /**
     * Build full invite link from invite code
     * @param inviteCode the invitation code
     * @return full invite link
     */
    private String buildInviteLink(String inviteCode) {
        if (inviteCode == null) {
            return null;
        }
        
        String baseUrl = appProperties.getClient().getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            log.warn("Client base URL not configured, returning invite code only");
            return inviteCode;
        }
        
        // Remove trailing slash if exists
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        return baseUrl + "/" + inviteCode;
    }
    
    private GroupMemberResponse mapToGroupMemberResponse(GroupMember groupMember) {
        User user = groupMember.getUser();
        GroupMemberResponse response = new GroupMemberResponse();
        response.setUserId(user.getUserId());
        response.setUserFullName(user.getUserFullName());
        response.setUserAvatarUrl(user.getUserAvatarUrl());
        response.setRole(groupMember.getRole());
        return response;
    }
} 