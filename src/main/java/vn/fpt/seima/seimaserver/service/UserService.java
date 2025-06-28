package vn.fpt.seima.seimaserver.service;

import jakarta.validation.Valid;
import vn.fpt.seima.seimaserver.dto.request.user.UserCreationRequestDto;
import vn.fpt.seima.seimaserver.dto.request.user.UserUpdateRequestDto;
import vn.fpt.seima.seimaserver.entity.User;


public interface UserService {



    void processAddNewUser(@Valid UserCreationRequestDto userCreationRequestDto);

    User updateUserProfile(Integer userId, @Valid UserUpdateRequestDto userUpdateRequestDto);
    
    /**
     * Deactivate a user account by setting userIsActive to false
     * @param userId the ID of the user to deactivate
     * @throws IllegalArgumentException if userId is null
     * @throws RuntimeException if user not found
     */
    void deactivateUserAccount(Integer userId);
}
