package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupResponse;
import vn.fpt.seima.seimaserver.entity.Group;
import vn.fpt.seima.seimaserver.entity.GroupMember;
import vn.fpt.seima.seimaserver.entity.GroupMemberRole;
import vn.fpt.seima.seimaserver.entity.GroupMemberStatus;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.GroupException;
import vn.fpt.seima.seimaserver.mapper.GroupMapper;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.repository.GroupRepository;
import vn.fpt.seima.seimaserver.service.CloudinaryService;
import vn.fpt.seima.seimaserver.service.GroupService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {
    
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMapper groupMapper;
    private final CloudinaryService cloudinaryService;
    
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
        
        // Handle image upload if provided
        String avatarUrl = null;
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            log.info("Processing image upload for group");
            validateImageFile(request.getImage());
            avatarUrl = uploadImageToCloudinary(request.getImage());
            log.info("Group avatar uploaded successfully: {}", avatarUrl);
        }
        
        // Create group entity
        Group group = createGroupEntity(request);
        // Set the uploaded image URL if available
        if (avatarUrl != null) {
            group.setGroupAvatarUrl(avatarUrl);
        }
        
        Group savedGroup = groupRepository.save(group);
        log.info("Group created with ID: {}", savedGroup.getGroupId());
        
        // Add creator as admin member
        createAdminMembership(savedGroup, currentUser);
        
        GroupResponse response = groupMapper.toResponse(savedGroup);
        log.info("Group creation completed successfully");
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
            log.error("Failed to upload group avatar to Cloudinary", e);
            throw new GroupException("Failed to upload group avatar: " + e.getMessage());
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
        return group;
    }
    
    private void createAdminMembership(Group group, User user) {
        GroupMember groupMember = new GroupMember();
        groupMember.setGroup(group);
        groupMember.setUser(user);
        groupMember.setRole(GroupMemberRole.ADMIN);
        groupMember.setStatus(GroupMemberStatus.ACTIVE);
        
        GroupMember savedMember = groupMemberRepository.save(groupMember);
        log.info("Admin membership created for user ID: {} in group ID: {}", 
                user.getUserId(), group.getGroupId());
    }
} 