package vn.fpt.seima.seimaserver.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.user.UserCreationRequestDto;
import vn.fpt.seima.seimaserver.exception.GmailAlreadyExistException;
import vn.fpt.seima.seimaserver.exception.NotMatchCurrentGmailException;
import vn.fpt.seima.seimaserver.service.UserService;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private UserService userService;


    @PostMapping("add-new-user")
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
}
