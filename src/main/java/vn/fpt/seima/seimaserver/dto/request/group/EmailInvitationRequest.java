package vn.fpt.seima.seimaserver.dto.request.group;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailInvitationRequest {
    
    @NotNull(message = "Group ID is required")
    private Integer groupId;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
} 