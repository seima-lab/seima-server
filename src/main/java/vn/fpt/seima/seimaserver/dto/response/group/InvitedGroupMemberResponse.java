package vn.fpt.seima.seimaserver.dto.response.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for invited group members
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitedGroupMemberResponse {
    
    /**
     * ID of the invited user
     */
    private Integer userId;
    
    /**
     * Email of the invited user
     */
    private String userEmail;
    
    /**
     * Full name of the invited user
     */
    private String userFullName;
    
    /**
     * Avatar URL of the invited user
     */
    private String userAvatarUrl;
    
    /**
     * When the invitation was sent
     */
    private LocalDateTime invitedAt;
    
    /**
     * Role that will be assigned when user accepts invitation
     */
    private String assignedRole;
} 