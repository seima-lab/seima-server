package vn.fpt.seima.seimaserver.dto.request.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateGroupRequest {
    @NotBlank(message = "Group name is required")
    @Size(max = 100, message = "Group name cannot exceed 100 characters")
    private String groupName;
    
    @Size(max = 512, message = "Group avatar URL cannot exceed 512 characters")
    private String groupAvatarUrl;
} 