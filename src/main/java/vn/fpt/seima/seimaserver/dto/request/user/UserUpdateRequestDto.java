package vn.fpt.seima.seimaserver.dto.request.user;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Data
public class UserUpdateRequestDto {
    // Email thường không cho phép cập nhật qua đây hoặc cần xử lý đặc biệt.
    // private String email;

    @Size(min = 1, message = "Full name cannot be empty if provided")
    private String fullName; // Cho phép null nếu không muốn cập nhật

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    @Pattern(regexp = "^(0[3|5|7|8|9])([0-9]{8})$", message = "Invalid Vietnamese phone number format")
    @Size(min=10, max = 11, message = "Phone number must be 10 or 11 digits")
    private String phoneNumber;

    private Boolean gender; // Cho phép null nếu không muốn cập nhật
    
    /**
     * Image file for profile avatar upload (optional).
     * If provided, will replace current user avatar.
     * If not provided, current avatar will be kept.
     */
    private MultipartFile image;
    
    /**
     * Flag to indicate if user wants to remove current avatar.
     * Only applicable when no new image is provided.
     */
    private Boolean removeCurrentAvatar = false;
}