package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
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
    public void processAddNewUser(UserCreationRequestDto userCreationRequestDto) {


        User currentUser = UserUtils.getCurrentUser();

        if (currentUser == null) {

            throw new IllegalStateException("Authenticated user not found. Cannot process user creation/update.");
        }


        if (!currentUser.getUserEmail().equals(userCreationRequestDto.getEmail())) {
            logger.warn("Attempt to update profile for email {} with data for email {}. Denied.",
                    currentUser.getUserEmail(), userCreationRequestDto.getEmail());
            throw new NotMatchCurrentGmailException("Email in request does not match the authenticated user's email.");
        }


        currentUser.setUserFullName(userCreationRequestDto.getFullName());
        currentUser.setUserGender(userCreationRequestDto.isGender());
        currentUser.setUserDob(userCreationRequestDto.getBirthDate());
        currentUser.setUserPhoneNumber(userCreationRequestDto.getPhoneNumber());
        // Chỉ cập nhật avatar nếu có giá trị mới, tránh ghi đè null/empty string nếu không muốn
        if (userCreationRequestDto.getAvatarUrl() != null && !userCreationRequestDto.getAvatarUrl().isEmpty()) {
            currentUser.setUserAvatarUrl(userCreationRequestDto.getAvatarUrl());
        }
        currentUser.setUserIsActive(true); // Đảm bảo người dùng được active

        // Lưu lại currentUser. Vì currentUser là một managed entity (nếu UserUtils trả về đúng),
        // lệnh save này sẽ tương ứng với một SQL UPDATE.
        userRepository.save(currentUser);
        logger.info("User profile updated successfully for email: {}", currentUser.getUserEmail());
    }

    @Override
    public User updateUserProfile(Integer userId, UserUpdateRequestDto dto) {
        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId)); // Hoặc exception phù hợp

        // Cập nhật các trường nếu DTO cung cấp giá trị mới (khác null)
        if (dto.getFullName() != null) {
            userToUpdate.setUserFullName(dto.getFullName());
        }
        if (dto.getBirthDate() != null) {
            userToUpdate.setUserDob(dto.getBirthDate());
        }
        if (dto.getPhoneNumber() != null) {
            userToUpdate.setUserPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getAvatarUrl() != null) {
            userToUpdate.setUserAvatarUrl(dto.getAvatarUrl());
        }
        if (dto.getGender() != null) {
            userToUpdate.setUserGender(dto.getGender());
        }


        return userRepository.save(userToUpdate);
    }
}