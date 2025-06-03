package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.request.user.UserCreateDto;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.GmailAlreadyExistException;
import vn.fpt.seima.seimaserver.exception.NotMatchCurrentGmailException;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.UserService;

import java.util.Optional;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private UserRepository userRepository;
    private SecurityService securityService;

    @Override
    public void createUser(UserCreateDto userCreateDto) {
        // Kiểm tra xem người dùng đã tồn tại chưa
        Optional<User> existingUser = userRepository.findByUserEmail(userCreateDto.getEmail());
        if (existingUser.isPresent()) {
            logger.warn("User with email {} already exists.", userCreateDto.getEmail());
            throw new GmailAlreadyExistException("User with this email already exists.");
        }
        // Kiểm tra xem người dùng có add email người khác không phải của mình không
        logger.warn("good"+ securityService.getCurrentUserEmail());
        Optional<String> currentUserEmailOpt = securityService.getCurrentUserEmail();
        if (currentUserEmailOpt.isPresent()) {
            if (!currentUserEmailOpt.get().equals(userCreateDto.getEmail())) {
                logger.warn("Attempt to create user with email {} that does not match current authenticated user's email {}.",
                        userCreateDto.getEmail(), currentUserEmailOpt.get());
                throw new NotMatchCurrentGmailException("You can only create a user profile for your authenticated email address.");
            }
        } else {
            logger.warn("Unauthenticated or user email not found in context, cannot verify user creation for email {}", userCreateDto.getEmail());
            throw new NotMatchCurrentGmailException("Unable to determine current user's email for verification.");
        }
        User newUser = User
                .builder()
                .userFullName(userCreateDto.getFullName())
                .userEmail(userCreateDto.getEmail())
                .userDob(userCreateDto.getBirthDate())
                .userGender(userCreateDto.isGender())
                .userPhoneNumber(userCreateDto.getPhoneNumber())
                .userAvatarUrl(securityService.getCurrentUserAvatarUrl().orElse(null))
                .userIsActive(true)
                .build();
        logger.info("Creating new user with email: {}", newUser.getUserEmail());
        userRepository.save(newUser);
    }

    public User getCurrentUser() {
        String currentEmail = securityService.getCurrentUserEmail()
                .orElseThrow(() -> new RuntimeException("Current user email not found in security context."));
        return userRepository.findByUserEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("Current user not found in DB with email: " + currentEmail));
    }
}
