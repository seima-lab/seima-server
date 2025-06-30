package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fpt.seima.seimaserver.dto.request.user.UserCreationRequestDto;
import vn.fpt.seima.seimaserver.dto.request.user.UserUpdateRequestDto;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.NotMatchCurrentGmailException;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.UserService;
import vn.fpt.seima.seimaserver.util.UserUtils;

// import java.util.Optional; // Có thể cần

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private UserRepository userRepository;


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
        logger.info("User profile updated successfully for email: {}", currentUser.getUserEmail());
    }

    @Override
    @Transactional
    public User updateUserProfile(Integer userId, UserUpdateRequestDto dto) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Cập nhật các trường nếu DTO cung cấp giá trị mới (khác null)
        if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
            userToUpdate.setUserFullName(dto.getFullName().trim());
        }
        if (dto.getBirthDate() != null) {
            userToUpdate.setUserDob(dto.getBirthDate());
        }
        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().trim().isEmpty()) {
            userToUpdate.setUserPhoneNumber(dto.getPhoneNumber().trim());
        }
        if (dto.getAvatarUrl() != null && !dto.getAvatarUrl().trim().isEmpty()) {
            userToUpdate.setUserAvatarUrl(dto.getAvatarUrl().trim());
        }
        if (dto.getGender() != null) {
            userToUpdate.setUserGender(dto.getGender());
        }

        // Đảm bảo user được active sau khi cập nhật profile (đặc biệt quan trọng cho Google login users)
        userToUpdate.setUserIsActive(true);

        User savedUser = userRepository.save(userToUpdate);
        logger.info("User profile updated successfully and activated for userId: {}", userId);
        return savedUser;
    }

    @Override
    @Transactional
    public void deactivateUserAccount(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        User userToDeactivate = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Check if user is already inactive
        if (!Boolean.TRUE.equals(userToDeactivate.getUserIsActive())) {
            logger.warn("User {} is already inactive", userId);
            return;
        }
        
        // Deactivate the user
        userToDeactivate.setUserIsActive(false);
        userRepository.save(userToDeactivate);
        
        logger.info("User account deactivated successfully for userId: {}", userId);
    }
}