package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import vn.fpt.seima.seimaserver.config.base.AppProperties;
import vn.fpt.seima.seimaserver.dto.request.group.CancelJoinGroupRequest;
import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.request.group.UpdateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.*;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.exception.GroupException;
import vn.fpt.seima.seimaserver.mapper.GroupMapper;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.repository.GroupRepository;
import vn.fpt.seima.seimaserver.service.CloudinaryService;
import vn.fpt.seima.seimaserver.service.GroupService;
import vn.fpt.seima.seimaserver.service.GroupPermissionService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {
    
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMapper groupMapper;
    private final CloudinaryService cloudinaryService;
    private final AppProperties appProperties;
    private final GroupPermissionService groupPermissionService;
    
    // Constants for image validation
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    @Override
    @Transactional
    public GroupResponse createGroupWithImage(CreateGroupRequest request) {
        // Validate request first
        validateCreateGroupRequest(request);
        
        log.info("Creating group with name: {} and optional image", request.getGroupName());
        
        // Get current user
        User currentUser = getCurrentUser();
        
        // Handle avatar upload (optional)
        String avatarUrl;
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            log.info("Processing image upload for group");
            validateImageFile(request.getImage());
            avatarUrl = uploadImageToCloudinary(request.getImage());
            log.info("Group avatar uploaded successfully: {}", avatarUrl);
        } else {
            // Set default group avatar URL when no image is provided
            avatarUrl = "https://cdn.pixabay.com/photo/2016/11/14/17/39/group-1824145_1280.png";
            log.info("No image provided, using default group avatar: {}", avatarUrl);
        }
        
        // Create group entity
        Group group = createGroupEntity(request);
        // Set the avatar URL (either uploaded or default)
        group.setGroupAvatarUrl(avatarUrl);
        
        Group savedGroup = groupRepository.save(group);
        log.info("Group created with ID: {}", savedGroup.getGroupId());
        
        // Add creator as owner member
        createOwnerMembership(savedGroup, currentUser);
        
        GroupResponse response = groupMapper.toResponse(savedGroup);
        log.info("Group creation completed successfully");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public GroupDetailResponse getGroupDetail(Integer groupId) {
        log.info("Getting group detail for group ID: {}", groupId);
        
        // Validate input
        if (groupId == null) {
            throw new GroupException("Group ID cannot be null");
        }
        
        // Get current user
        User currentUser = getCurrentUser();
        
        // Find group
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new GroupException("Group not found"));
        
        // Check if group is active - nếu không active thì báo group không tồn tại
        if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
            throw new GroupException("Group not found");
        }
        
        // Get current user's membership and role
        Optional<GroupMember> currentUserMembership = groupMemberRepository.findByUserAndGroupAndStatus(
                currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE);
        
        if (currentUserMembership.isEmpty()) {
            throw new GroupException("You don't have permission to view this group");
        }
        
        GroupMemberRole currentUserRole = currentUserMembership.get().getRole();
        
        // Check permission to view group members
        if (!groupPermissionService.canViewGroupMembers(currentUserRole)) {
            throw new GroupException("You don't have permission to view group members");
        }
        
        // Get group leader (owner)
        Optional<GroupMember> leaderOptional = groupMemberRepository.findGroupOwner(
            groupId, GroupMemberStatus.ACTIVE);
        
        if (leaderOptional.isEmpty()) {
            throw new GroupException("Group owner not found for group ID: " + groupId);
        }
        
        GroupMember leader = leaderOptional.get();
        
        // Check if leader's account is active
        if (!Boolean.TRUE.equals(leader.getUser().getUserIsActive())) {
            throw new GroupException("Group owner account is inactive for group ID: " + groupId);
        }
        
        GroupMemberResponse leaderResponse = mapToGroupMemberResponse(leader);
        
        // Get all active members (including leader)
        List<GroupMember> allMembers = groupMemberRepository.findActiveGroupMembers(
            groupId, GroupMemberStatus.ACTIVE);
        
        // Filter out inactive users and exclude leader from members list
        List<GroupMemberResponse> memberResponses = allMembers.stream()
            .filter(member -> Boolean.TRUE.equals(member.getUser().getUserIsActive())) // Filter active users only
            .filter(member -> !member.getUser().getUserId().equals(leader.getUser().getUserId()))
            .map(this::mapToGroupMemberResponse)
            .collect(Collectors.toList());
        
        // Get total member count (only active users)
        Long totalActiveMembers = allMembers.stream()
            .filter(member -> Boolean.TRUE.equals(member.getUser().getUserIsActive()))
            .count();

        // Build invite link
        String inviteLink = buildInviteLink(group.getGroupInviteCode());
        
        // Build response
        GroupDetailResponse response = new GroupDetailResponse();
        response.setGroupId(group.getGroupId());
        response.setGroupName(group.getGroupName());
        response.setGroupInviteLink(inviteLink);
        response.setGroupAvatarUrl(group.getGroupAvatarUrl());
        response.setGroupCreatedDate(group.getGroupCreatedDate());
        response.setGroupIsActive(group.getGroupIsActive());
        response.setGroupLeader(leaderResponse);
        response.setMembers(memberResponses);
        response.setTotalMembersCount(totalActiveMembers.intValue());
        response.setCurrentUserRole(currentUserRole); // Set current user's role

        log.info("Successfully retrieved group detail for group ID: {} with {} total active members, current user role: {}",
                groupId, totalActiveMembers, currentUserRole);

        return response;
    }

    @Override
    @Transactional
    public GroupResponse updateGroupInformation(Integer groupId, UpdateGroupRequest request) {
        log.info("Updating group information for group ID: {}", groupId);

        // Validate input
        validateUpdateGroupRequest(groupId, request);

        // Get current user
        User currentUser = getCurrentUser();

        // Find and validate group
        Group group = findAndValidateGroupForUpdate(groupId, currentUser);

        // Get current user's role and check permissions
        GroupMemberRole currentUserRole = getCurrentUserRole(groupId, currentUser);
        
        // Check permission to update group info using GroupPermissionService
        if (!groupPermissionService.canUpdateGroupInfo(currentUserRole)) {
            String permissionDesc = groupPermissionService.getPermissionDescription(
                "UPDATE_GROUP_INFO", currentUserRole, null);
            log.warn("Permission denied: {}", permissionDesc);
            throw new GroupException("Only group owners can update group information");
        }

        // Process group name update
        boolean nameUpdated = updateGroupName(group, request.getGroupName());

        // Process avatar update
        boolean avatarUpdated = updateGroupAvatar(group, request);

        // Save group if any changes were made
        if (nameUpdated || avatarUpdated) {
            group = groupRepository.save(group);
            log.info("Group information updated successfully for group ID: {}", groupId);
        } else {
            log.info("No changes detected for group ID: {}", groupId);
        }

        GroupResponse response = groupMapper.toResponse(group);
        log.info("Group update completed successfully for group ID: {}", groupId);
        return response;
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
    
    private void validateCreateGroupRequest(CreateGroupRequest request) {
        if (request == null) {
            throw new GroupException("Group request cannot be null");
        }
        
        if (!StringUtils.hasText(request.getGroupName())) {
            throw new GroupException("Group name is required and cannot be empty");
        }
        
        if (request.getGroupName().trim().length() > 100) {
            throw new GroupException("Group name cannot exceed 100 characters");
        }
    }
    
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GroupException("Image file cannot be null or empty");
        }
        
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new GroupException("Image file size must be less than 5MB");
        }
        
        // Check file format
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new GroupException("Invalid image file");
        }
        
        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        if (!SUPPORTED_FORMATS.contains(fileExtension)) {
            throw new GroupException("Unsupported image format. Supported formats: " + String.join(", ", SUPPORTED_FORMATS));
        }
        
        // Check MIME type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new GroupException("File must be an image");
        }
    }
    
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
    
    private String uploadImageToCloudinary(MultipartFile image) {
        try {
            Map uploadResult = cloudinaryService.uploadImage(image, "groups");
            return (String) uploadResult.get("secure_url");
        } catch (Exception e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new GroupException("Failed to upload image: " + e.getMessage());
        }
    }
    
    private User getCurrentUser() {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new GroupException("Unable to identify the current user");
        }
        return currentUser;
    }
    
    private Group createGroupEntity(CreateGroupRequest request) {
        Group group = groupMapper.toEntity(request);
        // Ensure default values are set
        if (group.getGroupIsActive() == null) {
            group.setGroupIsActive(true);
        }
        // Generate unique invite code
        group.setGroupInviteCode(generateInviteCode());
        return group;
    }
    
    /**
     * Generates a unique invite code for the group.
     * Uses UUID to ensure uniqueness and removes hyphens for cleaner code.
     * 
     * @return a unique 32-character invite code
     */
    private String generateInviteCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    private void createOwnerMembership(Group group, User user) {
        GroupMember groupMember = new GroupMember();
        groupMember.setGroup(group);
        groupMember.setUser(user);
        groupMember.setRole(GroupMemberRole.OWNER);  // Group creator becomes OWNER
        groupMember.setStatus(GroupMemberStatus.ACTIVE);
        
        GroupMember savedMember = groupMemberRepository.save(groupMember);
        log.info("Owner membership created for user ID: {} in group ID: {}", 
                user.getUserId(), group.getGroupId());
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

    private void validateUpdateGroupRequest(Integer groupId, UpdateGroupRequest request) {
        if (groupId == null) {
            throw new GroupException("Group ID cannot be null");
        }

        if (request == null) {
            throw new GroupException("Update request cannot be null");
        }

        // At least one field should be provided for update
        if (!hasUpdateData(request)) {
            throw new GroupException("At least one field (groupName or image) must be provided for update");
        }

        // Validate group name if provided
        if (request.getGroupName() != null) {
            String trimmedName = request.getGroupName().trim();
            if (trimmedName.isEmpty()) {
                throw new GroupException("Group name cannot be empty");
            }
            if (trimmedName.length() > 100) {
                throw new GroupException("Group name cannot exceed 100 characters");
            }
        }

        // Validate image if provided
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            validateImageFile(request.getImage());
        }

        // Validate remove avatar flag
        if (request.getImage() != null && !request.getImage().isEmpty() &&
                Boolean.TRUE.equals(request.getRemoveCurrentAvatar())) {
            throw new GroupException("Cannot provide new image and remove current avatar at the same time");
        }
    }

    private boolean hasUpdateData(UpdateGroupRequest request) {
        return (request.getGroupName() != null && !request.getGroupName().trim().isEmpty()) ||
                (request.getImage() != null && !request.getImage().isEmpty()) ||
                Boolean.TRUE.equals(request.getRemoveCurrentAvatar());
    }

    private Group findAndValidateGroupForUpdate(Integer groupId, User currentUser) {
        // Find group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException("Group not found"));

        // Check if group is active
        if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
            throw new GroupException("Group not found");
        }

        return group;
    }

    /**
     * Get current user's role in the group
     * @param groupId the group ID
     * @param currentUser the current user
     * @return the user's role in the group
     */
    private GroupMemberRole getCurrentUserRole(Integer groupId, User currentUser) {
        Optional<GroupMember> membership = groupMemberRepository.findByUserAndGroupAndStatus(
                currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE);
        
        if (membership.isEmpty()) {
            throw new GroupException("You are not a member of this group");
        }
        
        return membership.get().getRole();
    }

    /**
     * Validate admin or owner permission for group operations
     * Uses GroupPermissionService to check manage group settings permission
     */
    private void validateManageGroupPermission(Integer groupId, User currentUser) {
        GroupMemberRole currentUserRole = getCurrentUserRole(groupId, currentUser);
        
        if (!groupPermissionService.canManageGroupSettings(currentUserRole)) {
            String permissionDesc = groupPermissionService.getPermissionDescription(
                "MANAGE_GROUP_SETTINGS", currentUserRole, null);
            log.warn("Permission denied: {}", permissionDesc);
            throw new GroupException("Only group administrators and owners can manage group settings");
        }
    }

    private boolean updateGroupName(Group group, String newGroupName) {
        if (newGroupName == null || newGroupName.trim().isEmpty()) {
            return false; // No update needed
        }

        String trimmedNewName = newGroupName.trim();
        String currentName = group.getGroupName();

        // Check if name actually changed
        if (trimmedNewName.equals(currentName)) {
            log.info("Group name unchanged: {}", currentName);
            return false;
        }

        group.setGroupName(trimmedNewName);
        log.info("Group name updated from '{}' to '{}'", currentName, trimmedNewName);
        return true;
    }

    private boolean updateGroupAvatar(Group group, UpdateGroupRequest request) {
        // Handle remove current avatar case
        if (Boolean.TRUE.equals(request.getRemoveCurrentAvatar()) &&
                (request.getImage() == null || request.getImage().isEmpty())) {
            if (group.getGroupAvatarUrl() != null) {
                String oldAvatarUrl = group.getGroupAvatarUrl();
                group.setGroupAvatarUrl(null);
                log.info("Group avatar removed. Old URL: {}", oldAvatarUrl);
                return true;
            }
            return false; // No avatar to remove
        }

        // Handle new image upload case
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String oldAvatarUrl = group.getGroupAvatarUrl();
            String newAvatarUrl = uploadImageToCloudinary(request.getImage());
            group.setGroupAvatarUrl(newAvatarUrl);
            log.info("Group avatar updated. Old URL: {}, New URL: {}", oldAvatarUrl, newAvatarUrl);
            return true;
        }

        return false; // No avatar update
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserJoinedGroupResponse> getUserJoinedGroups() {
        log.info("Getting joined groups for current user");

        // Get current user
        User currentUser = getCurrentUser();

        // Find all groups where user has active membership and group is active
        List<GroupMember> userGroupMemberships = groupMemberRepository.findUserJoinedGroups(
                currentUser.getUserId(),
                GroupMemberStatus.ACTIVE,
                true
        );

        // Convert to response DTOs
        List<UserJoinedGroupResponse> responses = userGroupMemberships.stream()
                .map(this::mapToUserJoinedGroupResponse)
                .collect(Collectors.toList());

        log.info("Found {} joined groups for user ID: {}", responses.size(), currentUser.getUserId());
        return responses;
    }

    private UserJoinedGroupResponse mapToUserJoinedGroupResponse(GroupMember groupMember) {
        Group group = groupMember.getGroup();

        // Get group leader (owner)
        Optional<GroupMember> leaderOptional = groupMemberRepository.findGroupOwner(
                group.getGroupId(), GroupMemberStatus.ACTIVE);

        GroupMemberResponse leaderResponse = null;
        if (leaderOptional.isPresent()) {
            GroupMember leaderMember = leaderOptional.get();
            // Only include leader if their account is active
            if (Boolean.TRUE.equals(leaderMember.getUser().getUserIsActive())) {
                leaderResponse = mapToGroupMemberResponse(leaderMember);
            }
        }

        // Get total member count (only active users)
        List<GroupMember> allActiveMembers = groupMemberRepository.findActiveGroupMembers(
                group.getGroupId(), GroupMemberStatus.ACTIVE);
        
        Long totalActiveCount = allActiveMembers.stream()
                .filter(member -> Boolean.TRUE.equals(member.getUser().getUserIsActive()))
                .count();

        return UserJoinedGroupResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .groupAvatarUrl(group.getGroupAvatarUrl())
                .groupCreatedDate(group.getGroupCreatedDate())
                .joinedDate(groupMember.getJoinDate())
                .userRole(groupMember.getRole())
                .totalMembersCount(totalActiveCount.intValue())
                .groupLeader(leaderResponse)
                .build();
    }

    @Override
    @Transactional
    public GroupResponse archiveGroup(Integer groupId) {
        log.info("Archiving group with ID: {}", groupId);

        // Validate input
        if (groupId == null) {
            throw new GroupException("Group ID cannot be null");
        }

        // Get current user
        User currentUser = getCurrentUser();

        // Find and validate group
        Group group = findAndValidateGroupForArchive(groupId);

        // Check permission using GroupPermissionService
        GroupMemberRole currentUserRole = getCurrentUserRole(groupId, currentUser);
        
        if (!groupPermissionService.canManageGroupSettings(currentUserRole)) {
            String permissionDesc = groupPermissionService.getPermissionDescription(
                "ARCHIVE_GROUP", currentUserRole, null);
            log.warn("Permission denied: {}", permissionDesc);
            throw new GroupException("Only group administrators and owners can archive groups");
        }

        // Archive the group
        group.setGroupIsActive(false);
        Group savedGroup = groupRepository.save(group);

        log.info("Group successfully archived with ID: {}", groupId);

        GroupResponse response = groupMapper.toResponse(savedGroup);
        log.info("Group archive completed successfully for group ID: {}", groupId);
        return response;
    }

    private Group findAndValidateGroupForArchive(Integer groupId) {
        // Find group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException("Group not found"));

        // Check if group is already archived
        if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
            throw new GroupException("Group is already archived");
        }

        return group;
    }

    @Override
    @Transactional(readOnly = true)
    public GroupMemberStatusResponse getCurrentUserGroupStatus(Integer groupId) {
        log.info("Getting current user status for group ID: {}", groupId);
        
        // Validate input
        if (groupId == null) {
            throw new GroupException("Group ID cannot be null");
        }
        
        // Get current user
        User currentUser = getCurrentUser();
        
        // Find group
        Optional<Group> groupOpt = groupRepository.findById(groupId);
        
        if (groupOpt.isEmpty()) {
            // Group not found
            return GroupMemberStatusResponse.builder()
                    .groupId(groupId)
                    .status(null)
                    .role(null)
                    .groupExists(false)
                    .build();
        }
        
        Group group = groupOpt.get();
        
        // Check if group is inactive
        if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
            return GroupMemberStatusResponse.builder()
                    .groupId(groupId)
                    .status(null)
                    .role(null)
                    .groupExists(false)
                    .build();
        }
        
        // Find user's membership in this group
        Optional<GroupMember> membershipOpt = groupMemberRepository.findByUserIdAndGroupId(
                currentUser.getUserId(), groupId);
        
        if (membershipOpt.isEmpty()) {
            // User is not a member
            return GroupMemberStatusResponse.builder()
                    .groupId(groupId)
                    .status(null)
                    .role(null)
                    .groupExists(true)
                    .build();
        }
        
        GroupMember membership = membershipOpt.get();
        
        // Return user's current status
        GroupMemberStatusResponse response = GroupMemberStatusResponse.builder()
                .groupId(groupId)
                .status(membership.getStatus())
                .role(membership.getRole())
                .groupExists(true)
                .build();
        
        log.info("User {} status in group {}: status={}, role={}", 
                currentUser.getUserId(), groupId, membership.getStatus(), membership.getRole());
        
        return response;
    }

    @Override
    @Transactional
    public void deleteGroup(Integer groupId) {
        log.info("Deleting group with ID: {}", groupId);

        // Validate input
        if (groupId == null) {
            throw new GroupException("Group ID cannot be null");
        }

        // Get current user
        User currentUser = getCurrentUser();

        // Find and validate group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException("Group not found"));

        // Check if group is already inactive
        if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
            throw new GroupException("Group is already inactive");
        }

        // Validate current user is owner
        GroupMemberRole currentUserRole = getCurrentUserRole(groupId, currentUser);
        if (currentUserRole != GroupMemberRole.OWNER) {
            throw new GroupException("Only group owner can delete the group");
        }

        // Soft delete: Set group as inactive instead of hard delete to preserve data integrity
        group.setGroupIsActive(false);
        groupRepository.save(group);

        // Set all active members to LEFT status
        List<GroupMember> activeMembers = groupMemberRepository.findActiveGroupMembers(
                groupId, GroupMemberStatus.ACTIVE);
        
        for (GroupMember member : activeMembers) {
            member.setStatus(GroupMemberStatus.LEFT);
            groupMemberRepository.save(member);
        }

        log.info("Successfully deleted group {} and set all {} members to LEFT status", 
                groupId, activeMembers.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPendingGroupResponse> getUserPendingGroups() {
        log.info("Getting pending groups for current user");
        
        // Get current user
        User currentUser = getCurrentUser();
        
        // Find all groups where user has PENDING_APPROVAL status
        List<GroupMember> pendingMemberships = groupMemberRepository.findUserPendingGroups(
                currentUser.getUserId(), GroupMemberStatus.PENDING_APPROVAL);
        
        // Map to response DTOs
        List<UserPendingGroupResponse> pendingGroups = pendingMemberships.stream()
                .map(this::mapToUserPendingGroupResponse)
                .collect(Collectors.toList());
        
        log.info("Found {} pending groups for user {}", pendingGroups.size(), currentUser.getUserId());
        
        return pendingGroups;
    }
    
    /**
     * Map GroupMember entity to UserPendingGroupResponse DTO
     */
    private UserPendingGroupResponse mapToUserPendingGroupResponse(GroupMember groupMember) {
        Group group = groupMember.getGroup();
        
        // Count active members in the group
        Long activeMemberCount = groupMemberRepository.countActiveGroupMembers(
                group.getGroupId(), GroupMemberStatus.ACTIVE);
        
        return UserPendingGroupResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .groupAvatarUrl(group.getGroupAvatarUrl())
                .groupIsActive(group.getGroupIsActive())
                .requestedAt(groupMember.getJoinDate())
                .activeMemberCount(activeMemberCount.intValue())
                .build();
    }

    @Override
    @Transactional
    public void cancelJoinGroupRequest(CancelJoinGroupRequest request) {
        // Validate input first
        validateCancelRequestInput(request);
        
        log.info("Canceling join group request for group ID: {}", request.getGroupId());
        
        // Get current user
        User currentUser = getCurrentUser();
        
        // Find and validate group
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new GroupException("Group not found"));
        
        // Check if group is active
        if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
            throw new GroupException("Group not found");
        }
        
        // Find user's pending membership in this group
        Optional<GroupMember> pendingMembershipOpt = groupMemberRepository.findByUserAndGroupAndStatus(
                currentUser.getUserId(), request.getGroupId(), GroupMemberStatus.PENDING_APPROVAL);
        
        if (pendingMembershipOpt.isEmpty()) {
            throw new GroupException("No pending join request found for this group");
        }
        
        GroupMember pendingMembership = pendingMembershipOpt.get();
        
        // Cancel the request by setting status to LEFT
        pendingMembership.setStatus(GroupMemberStatus.LEFT);
        groupMemberRepository.save(pendingMembership);
        
        log.info("Successfully canceled join request for user {} in group {}", 
                currentUser.getUserId(), request.getGroupId());
    }
    
    /**
     * Validate cancel join group request input
     */
    private void validateCancelRequestInput(CancelJoinGroupRequest request) {
        if (request == null) {
            throw new GroupException("Cancel request cannot be null");
        }
        
        if (request.getGroupId() == null) {
            throw new GroupException("Group ID cannot be null");
        }
        
        if (request.getGroupId() <= 0) {
            throw new GroupException("Group ID must be a positive integer");
        }
    }
} 