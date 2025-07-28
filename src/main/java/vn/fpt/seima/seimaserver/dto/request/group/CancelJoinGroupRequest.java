package vn.fpt.seima.seimaserver.dto.request.group;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for canceling a join group request
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancelJoinGroupRequest {
    
    /**
     * ID of the group to cancel join request
     * Required field
     */
    @NotNull(message = "Group ID cannot be null")
    private Integer groupId;
} 