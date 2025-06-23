package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.request.group.JoinGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.InvitationDetailsResponse;
import vn.fpt.seima.seimaserver.dto.response.group.JoinGroupResponse;

/**
 * Service interface for handling group invitations
 * Follows Single Responsibility Principle - only handles invitation-related operations
 */
public interface GroupInvitationService {
    
    /**
     * Get invitation details by invite code
     * @param inviteCode the invitation code
     * @return invitation details including group information
     */
    InvitationDetailsResponse getInvitationDetails(String inviteCode);
    
    /**
     * Join a group using invitation code
     * @param request the join group request containing invite code
     * @return join group response with updated group information
     */
    JoinGroupResponse joinGroupByInviteCode(JoinGroupRequest request);
    
    /**
     * Validate if an invitation code is valid and active
     * @param inviteCode the invitation code to validate
     * @return true if valid, false otherwise
     */
    boolean isInvitationValid(String inviteCode);
} 