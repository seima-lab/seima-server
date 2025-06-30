package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.request.group.EmailInvitationRequest;
import vn.fpt.seima.seimaserver.dto.request.group.JoinGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.EmailInvitationResponse;
import vn.fpt.seima.seimaserver.dto.response.group.InvitationDetailsResponse;
import vn.fpt.seima.seimaserver.dto.response.group.JoinGroupResponse;

/**
 * Service interface for handling group invitations
 * Follows Single Responsibility Principle - only handles invitation-related operations
 */
public interface GroupInvitationService {
    

    InvitationDetailsResponse getInvitationDetails(String inviteCode);
    

    JoinGroupResponse joinGroupByInviteCode(JoinGroupRequest request);
    

    boolean isInvitationValid(String inviteCode);
    
    /**
     * Send email invitation to invite user to join group
     * @param request Email invitation request containing groupId, email, and optional personal message
     * @return EmailInvitationResponse with invitation details and status
     */
    EmailInvitationResponse sendEmailInvitation(EmailInvitationRequest request);
} 