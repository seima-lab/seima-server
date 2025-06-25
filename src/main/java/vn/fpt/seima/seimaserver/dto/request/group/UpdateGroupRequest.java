package vn.fpt.seima.seimaserver.dto.request.group;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateGroupRequest {
    
    @Size(max = 100, message = "Group name cannot exceed 100 characters")
    private String groupName;
    
    /**
     * Image file for upload (optional).
     * If provided, will replace current group avatar.
     * If not provided, current avatar will be kept.
     */
    private MultipartFile image;
    
    /**
     * Flag to indicate if user wants to remove current avatar.
     * Only applicable when no new image is provided.
     */
    private Boolean removeCurrentAvatar = false;
} 