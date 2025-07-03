package vn.fpt.seima.seimaserver.dto.response.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.fpt.seima.seimaserver.entity.GroupMemberRole;
import vn.fpt.seima.seimaserver.entity.GroupMemberStatus;

/**
 * Response DTO for current user's status in a specific group
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberStatusResponse {
    
    /**
     * ID of the group being queried
     */
    private Integer groupId;
    
    /**
     * Current user's membership status in this group
     * Values: ACTIVE, PENDING_APPROVAL, INVITED, REJECTED, or null if not a member
     */
    private GroupMemberStatus status;
    
    /**
     * Current user's role in the group (only if status is ACTIVE)
     */
    private GroupMemberRole role;
    
    /**
     * Whether the group exists and is active
     */
    private boolean groupExists;
} 