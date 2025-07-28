package vn.fpt.seima.seimaserver.dto.response.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for groups that the current user has requested to join but are still pending approval
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPendingGroupResponse {
    

    private Integer groupId;
    

    private String groupName;
    

    private String groupAvatarUrl;
    

    private Boolean groupIsActive;
    

    private LocalDateTime requestedAt;
    

    private Integer activeMemberCount;
} 