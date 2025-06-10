package vn.fpt.seima.seimaserver.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.user.UserCreationRequestDto;
import vn.fpt.seima.seimaserver.dto.request.user.UserUpdateRequestDto;
import vn.fpt.seima.seimaserver.dto.response.user.UserProfileResponseDto;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.GmailAlreadyExistException;
import vn.fpt.seima.seimaserver.exception.NotMatchCurrentGmailException;
import vn.fpt.seima.seimaserver.mapper.UserMapper;
import vn.fpt.seima.seimaserver.service.UserService;
import vn.fpt.seima.seimaserver.util.UserUtils;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private UserService userService;


    @PostMapping("/create")
    public ApiResponse<Object> addNewUser(
            @Valid
            @RequestBody UserCreationRequestDto userCreationRequestDto
    ) {
        try {
            userService.processAddNewUser(userCreationRequestDto);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Add new user successfully")
                    .build();
        } catch (GmailAlreadyExistException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.CONFLICT.value())
                    .message(e.getMessage())
                    .build();
        } catch (NotMatchCurrentGmailException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.CONFLICT.value())
                    .message(e.getMessage())
                    .build();
        }
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponseDto> getCurrentUserProfile() {
        User currentUser = UserUtils.getCurrentUser(); // Hoặc inject CurrentUserUtil và gọi phương thức của nó
        if (currentUser == null) {
            return ApiResponse.<UserProfileResponseDto>builder()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .message("User not authenticated or profile not found.")
                    .build();
        }
        // Chuyển đổi User entity sang DTO để trả về
        UserProfileResponseDto userProfileDto = UserMapper.mapUserToProfileDto(currentUser);
        return ApiResponse.<UserProfileResponseDto>builder()
                .statusCode(HttpStatus.OK.value())
                .message("User profile fetched successfully")
                .data(userProfileDto)
                .build();
    }

    @PutMapping("/update")
    public ApiResponse<UserProfileResponseDto> updateCurrentUserProfile(
            @Valid @RequestBody UserUpdateRequestDto userUpdateRequestDto
    ) {
        try {
            User currentUser = UserUtils.getCurrentUser();
            if (currentUser == null) {
                return ApiResponse.<UserProfileResponseDto>builder()
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .message("User not authenticated.")
                        .build();
            }
            User updatedUser = userService.updateUserProfile(currentUser.getUserId(), userUpdateRequestDto); // Truyền userId
            UserProfileResponseDto userProfileDto = UserMapper.mapUserToProfileDto(updatedUser);
            return ApiResponse.<UserProfileResponseDto>builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("User profile updated successfully")
                    .data(userProfileDto)
                    .build();
        } catch (Exception e) {

            return ApiResponse.<UserProfileResponseDto>builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Error updating profile: " + e.getMessage())
                    .build();
        }
    }
}

