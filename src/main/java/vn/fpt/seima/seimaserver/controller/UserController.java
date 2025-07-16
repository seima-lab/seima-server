package vn.fpt.seima.seimaserver.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.user.MeRequestDto;
import vn.fpt.seima.seimaserver.dto.request.user.UserCreationRequestDto;
import vn.fpt.seima.seimaserver.dto.request.user.UserUpdateRequestDto;
import vn.fpt.seima.seimaserver.dto.response.user.UserProfileResponseDto;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.GmailAlreadyExistException;
import vn.fpt.seima.seimaserver.exception.NotMatchCurrentGmailException;
import vn.fpt.seima.seimaserver.mapper.UserMapper;
import vn.fpt.seima.seimaserver.repository.UserDeviceRepository;
import vn.fpt.seima.seimaserver.service.UserDeviceService;
import vn.fpt.seima.seimaserver.service.UserService;
import vn.fpt.seima.seimaserver.service.GroupMemberService;
import vn.fpt.seima.seimaserver.util.UserUtils;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private UserService userService;
    private GroupMemberService groupMemberService;
    private UserDeviceRepository userDeviceRepository;
    private UserDeviceService userDeviceService;


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
    public ApiResponse<UserProfileResponseDto> getCurrentUserProfile(
            @Valid
            @RequestBody MeRequestDto meRequestDto

    ) {
        User currentUser = UserUtils.getCurrentUser(); // Hoặc inject CurrentUserUtil và gọi phương thức của nó
        if (currentUser == null) {
            return ApiResponse.<UserProfileResponseDto>builder()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .message("User not authenticated or profile not found.")
                    .build();
        }
        // Lưu data vào bảng user_device với logic đúng
        if(userDeviceRepository.existsByDeviceId(meRequestDto.getDeviceId())) {
            // Device đã tồn tại → chỉ update thông tin
            userDeviceService.updateDeviceUser(meRequestDto.getDeviceId(), meRequestDto.getFcmToken());
        } else {
            // Device chưa tồn tại → tạo mới
            userDeviceService.createDevice(currentUser.getUserId(), meRequestDto.getDeviceId(), meRequestDto.getFcmToken());
        }

        // Chuyển đổi User entity sang DTO để trả về
        UserProfileResponseDto userProfileDto = UserMapper.mapUserToProfileDto(currentUser);
        return ApiResponse.<UserProfileResponseDto>builder()
                .statusCode(HttpStatus.OK.value())
                .message("User profile fetched successfully")
                .data(userProfileDto)
                .build();
    }

    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserProfileResponseDto> updateCurrentUserProfile(
            @ModelAttribute @Validated UserUpdateRequestDto userUpdateRequestDto
    ) {
        try {
            User currentUser = UserUtils.getCurrentUser();
            if (currentUser == null) {
                return ApiResponse.<UserProfileResponseDto>builder()
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .message("User not authenticated.")
                        .build();
            }
            User updatedUser = userService.updateUserProfileWithImage(currentUser.getUserId(), userUpdateRequestDto);
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

    @PutMapping("/deactivate")
    public ApiResponse<Object> deactivateCurrentUserAccount() {
        try {
            User currentUser = UserUtils.getCurrentUser();
            if (currentUser == null) {
                return ApiResponse.builder()
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .message("User not authenticated.")
                        .build();
            }

            // Handle group leadership transfer before deactivating
            groupMemberService.handleUserAccountDeactivation(currentUser.getUserId());
            
            // Deactivate the user account
            userService.deactivateUserAccount(currentUser.getUserId());
            
            return ApiResponse.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Account deactivated successfully. Group leadership has been transferred if applicable.")
                    .build();
                    
        } catch (Exception e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Error deactivating account: " + e.getMessage())
                    .build();
        }
    }
}

