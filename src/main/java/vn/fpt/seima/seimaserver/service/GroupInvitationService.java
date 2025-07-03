package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.request.group.EmailInvitationRequest;
import vn.fpt.seima.seimaserver.dto.response.group.EmailInvitationResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupInvitationLandingResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberResponse;


public interface GroupInvitationService {


    EmailInvitationResponse sendEmailInvitation(EmailInvitationRequest request);

    GroupInvitationLandingResponse processInvitationToken(String invitationToken);
} 