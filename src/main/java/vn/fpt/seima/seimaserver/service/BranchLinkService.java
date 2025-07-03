package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.response.group.BranchLinkResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberResponse;
import vn.fpt.seima.seimaserver.entity.Group;

/**
 * Service interface for Branch.io link operations
 */
public interface BranchLinkService {
    


    BranchLinkResponse createBranchLink(Group group, GroupMemberResponse leaderResponse);
    

    BranchLinkResponse createInvitationDeepLink(Integer groupId, Integer invitedUserId, Integer inviterId, String actionType);
} 