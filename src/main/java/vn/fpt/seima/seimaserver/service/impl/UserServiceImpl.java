package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.fpt.seima.seimaserver.dto.request.user.UserCreationRequestDto;
import vn.fpt.seima.seimaserver.dto.request.user.UserUpdateRequestDto;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.entity.UserDevice;
import vn.fpt.seima.seimaserver.exception.NotMatchCurrentGmailException;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.repository.UserDeviceRepository;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.CloudinaryService;
import vn.fpt.seima.seimaserver.service.UserDeviceService;
import vn.fpt.seima.seimaserver.service.UserService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

// import java.util.Optional; // Có thể cần

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final UserDeviceRepository userDeviceRepository;
    private final UserDeviceService userDeviceService;
    
    public UserServiceImpl(UserRepository userRepository, 
                          CloudinaryService cloudinaryService,
                          UserDeviceRepository userDeviceRepository,
                          @Lazy UserDeviceService userDeviceService) {
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
        this.userDeviceRepository = userDeviceRepository;
        this.userDeviceService = userDeviceService;
    }
    
    // Constants for image validation
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    @Override
    public User findUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public void processAddNewUser(UserCreationRequestDto userCreationRequestDto) {
        if (userCreationRequestDto == null) {
            throw new IllegalArgumentException("User creation request cannot be null");
        }

        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Authenticated user not found. Cannot process user creation/update.");
        }

        if (!currentUser.getUserEmail().equals(userCreationRequestDto.getEmail())) {
            logger.warn("Attempt to update profile for email {} with data for email {}. Denied.",
                    currentUser.getUserEmail(), userCreationRequestDto.getEmail());
            throw new NotMatchCurrentGmailException("Email in request does not match the authenticated user's email.");
        }

        // Update user information
        if (userCreationRequestDto.getFullName() != null && !userCreationRequestDto.getFullName().trim().isEmpty()) {
            currentUser.setUserFullName(userCreationRequestDto.getFullName().trim());
        }
        currentUser.setUserGender(userCreationRequestDto.isGender());
        currentUser.setUserDob(userCreationRequestDto.getBirthDate());
        
        if (userCreationRequestDto.getPhoneNumber() != null && !userCreationRequestDto.getPhoneNumber().trim().isEmpty()) {
            currentUser.setUserPhoneNumber(userCreationRequestDto.getPhoneNumber().trim());
        }
        
        // Chỉ cập nhật avatar nếu có giá trị mới, tránh ghi đè null/empty string nếu không muốn
        if (userCreationRequestDto.getAvatarUrl() != null && !userCreationRequestDto.getAvatarUrl().trim().isEmpty()) {
            currentUser.setUserAvatarUrl(userCreationRequestDto.getAvatarUrl().trim());
        }
        currentUser.setUserIsActive(true); // Đảm bảo người dùng được active

        // Lưu lại currentUser. Vì currentUser là một managed entity (nếu UserUtils trả về đúng),
        // lệnh save này sẽ tương ứng với một SQL UPDATE.
        userRepository.save(currentUser);

        // Lưu data vào bảng user_device với logic đúng
        if(userDeviceRepository.existsByDeviceId(userCreationRequestDto.getDeviceId())){
            // Device đã tồn tại → chỉ update thông tin
            userDeviceService.updateDeviceUser(currentUser.getUserId(),userCreationRequestDto.getDeviceId(), userCreationRequestDto.getFcmToken());
        } else {
            // Device chưa tồn tại → tạo mới
            userDeviceService.createDevice(currentUser.getUserId(), userCreationRequestDto.getDeviceId(), userCreationRequestDto.getFcmToken());
        }
        
        logger.info("User profile updated successfully for email: {}", currentUser.getUserEmail());
    }




    @Override
    @Transactional
    public User updateUserProfileWithImage(Integer userId, UserUpdateRequestDto dto) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        logger.info("Updating user profile with image support for userId: {}", userId);
        
        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Validate image update request
        validateImageUpdateRequest(dto);

        // Update basic profile fields
        updateBasicProfileFields(userToUpdate, dto);

        // Handle avatar update
        boolean avatarUpdated = updateUserAvatar(userToUpdate, dto);

        // Đảm bảo user được active sau khi cập nhật profile
        userToUpdate.setUserIsActive(true);

        User savedUser = userRepository.save(userToUpdate);
        logger.info("User profile updated successfully with image support for userId: {}, avatarUpdated: {}", 
                userId, avatarUpdated);
        return savedUser;
    }

    /**
     * Validate image update request
     */
    private void validateImageUpdateRequest(UserUpdateRequestDto dto) {
        // Validate image if provided
        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            validateImageFile(dto.getImage());
        }

        // Validate conflicting requests
        if (dto.getImage() != null && !dto.getImage().isEmpty() &&
                Boolean.TRUE.equals(dto.getRemoveCurrentAvatar())) {
            throw new IllegalArgumentException("Cannot provide new image and remove current avatar at the same time");
        }
    }

    /**
     * Update basic profile fields (non-image)
     */
    private void updateBasicProfileFields(User userToUpdate, UserUpdateRequestDto dto) {
        if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
            userToUpdate.setUserFullName(dto.getFullName().trim());
        }
        if (dto.getBirthDate() != null) {
            userToUpdate.setUserDob(dto.getBirthDate());
        }
        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().trim().isEmpty()) {
            userToUpdate.setUserPhoneNumber(dto.getPhoneNumber().trim());
        }
        if (dto.getGender() != null) {
            userToUpdate.setUserGender(dto.getGender());
        }
    }

    /**
     * Handle user avatar update logic
     */
    private boolean updateUserAvatar(User user, UserUpdateRequestDto dto) {
        // Handle remove current avatar case
        if (Boolean.TRUE.equals(dto.getRemoveCurrentAvatar()) &&
                (dto.getImage() == null || dto.getImage().isEmpty())) {
            if (user.getUserAvatarUrl() != null) {
                String oldAvatarUrl = user.getUserAvatarUrl();
                user.setUserAvatarUrl(null);
                logger.info("User avatar removed. UserId: {}, Old URL: {}", user.getUserId(), oldAvatarUrl);
                return true;
            }
            return false; // No avatar to remove
        }

        // Handle new image upload case
        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            String oldAvatarUrl = user.getUserAvatarUrl();
            String newAvatarUrl = uploadImageToCloudinary(dto.getImage());
            user.setUserAvatarUrl(newAvatarUrl);
            logger.info("User avatar updated. UserId: {}, Old URL: {}, New URL: {}", 
                    user.getUserId(), oldAvatarUrl, newAvatarUrl);
            return true;
        }

        return false; // No avatar update
    }

    /**
     * Validate image file format and size
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be null or empty");
        }
        
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image file size must be less than 5MB");
        }
        
        // Check file format
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Invalid image file");
        }
        
        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        if (!SUPPORTED_FORMATS.contains(fileExtension)) {
            throw new IllegalArgumentException("Unsupported image format. Supported formats: " + String.join(", ", SUPPORTED_FORMATS));
        }
        
        // Check MIME type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * Upload image to Cloudinary
     */
    private String uploadImageToCloudinary(MultipartFile image) {
        try {
            Map uploadResult = cloudinaryService.uploadImage(image, "users/avatars");
            return (String) uploadResult.get("secure_url");
        } catch (Exception e) {
            logger.error("Failed to upload user avatar to Cloudinary", e);
            throw new RuntimeException("Failed to upload avatar: " + e.getMessage());
        }
    }
}