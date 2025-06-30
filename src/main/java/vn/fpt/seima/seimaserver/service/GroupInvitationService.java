package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.request.group.JoinGroupRequest;
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
} 