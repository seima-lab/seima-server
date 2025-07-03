package vn.fpt.seima.seimaserver.dto.request.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing invitation token data stored in Redis
 * Contains all necessary information about a group invitation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationTokenData {
    
    /**
     * ID of the group being invited to
     */
    private Integer groupId;
    
    /**
     * ID of the user who sent the invitation
     */
    private Integer inviterId;
    
    /**
     * ID of the user who received the invitation
     */
    private Integer invitedUserId;
    
    /**
     * Email of the invited user
     */
    private String invitedUserEmail;
    
    /**
     * Current status of the invitation
     */
    private String status; // INVITED, ACCEPTED, REJECTED, EXPIRED
    
    /**
     * When the invitation was created
     */
    private LocalDateTime createdAt;
    
    /**
     * When the invitation expires (30 days from creation)
     */
    private LocalDateTime expiresAt;
    
    /**
     * Group name for reference
     */
    private String groupName;
    
    /**
     * Inviter name for reference
     */
    private String inviterName;
} 