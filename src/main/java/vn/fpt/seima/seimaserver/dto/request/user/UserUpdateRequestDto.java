package vn.fpt.seima.seimaserver.dto.request.user;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

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

    private String avatarUrl;
    private Boolean gender; // Cho phép null nếu không muốn cập nhật
}