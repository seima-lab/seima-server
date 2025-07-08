package vn.fpt.seima.seimaserver.service;

import jakarta.validation.Valid;
import vn.fpt.seima.seimaserver.dto.request.user.UserCreationRequestDto;
import vn.fpt.seima.seimaserver.dto.request.user.UserUpdateRequestDto;
import vn.fpt.seima.seimaserver.entity.User;


public interface UserService {



    void processAddNewUser(@Valid UserCreationRequestDto userCreationRequestDto);
    

    User updateUserProfileWithImage(Integer userId, @Valid UserUpdateRequestDto userUpdateRequestDto);
    

    void deactivateUserAccount(Integer userId);
}
