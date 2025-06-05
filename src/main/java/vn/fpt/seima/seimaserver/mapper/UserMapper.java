package vn.fpt.seima.seimaserver.mapper;

import vn.fpt.seima.seimaserver.dto.response.user.UserProfileResponseDto;
import vn.fpt.seima.seimaserver.entity.User;

public class UserMapper {
    public static UserProfileResponseDto mapUserToProfileDto(User user) {
        return UserProfileResponseDto.builder()
                .userId(user.getUserId())
                .userFullName(user.getUserFullName())
                .userEmail(user.getUserEmail())
                .userDob(user.getUserDob())
                .userGender(user.getUserGender())
                .userPhoneNumber(user.getUserPhoneNumber())
                .userAvatarUrl(user.getUserAvatarUrl())
                .build();
    }
}
