package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.request.group.UpdateGroupRequest;
import vn.fpt.seima.seimaserver.dto.request.group.CancelJoinGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.*;
import vn.fpt.seima.seimaserver.dto.response.group.UserPendingGroupResponse;
import vn.fpt.seima.seimaserver.dto.response.group.InvitedGroupMemberResponse;

import java.util.List;


public interface GroupService {

    GroupResponse createGroupWithImage(CreateGroupRequest request);


    GroupDetailResponse getGroupDetail(Integer groupId);


    GroupResponse updateGroupInformation(Integer groupId, UpdateGroupRequest request);


    List<UserJoinedGroupResponse> getUserJoinedGroups();


    GroupResponse archiveGroup(Integer groupId);
    

    GroupMemberStatusResponse getCurrentUserGroupStatus(Integer groupId);
    

    void deleteGroup(Integer groupId);
    

    List<UserPendingGroupResponse> getUserPendingGroups();
    

    void cancelJoinGroupRequest(CancelJoinGroupRequest request);
    

    List<InvitedGroupMemberResponse> getInvitedGroupMembers(Integer groupId);
}