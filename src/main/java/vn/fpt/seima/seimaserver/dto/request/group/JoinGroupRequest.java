package vn.fpt.seima.seimaserver.dto.request.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JoinGroupRequest {
    @NotBlank(message = "Invite code is required")
    @Size(min = 8, max = 36, message = "Invite code must be between 8 and 36 characters")
    private String inviteCode;
} 