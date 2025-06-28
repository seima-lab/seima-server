package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.request.group.UpdateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupDetailResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupResponse;
import vn.fpt.seima.seimaserver.dto.response.group.UserJoinedGroupResponse;
import vn.fpt.seima.seimaserver.exception.GroupException;

import java.util.List;

// import vn.fpt.seima.seimaserver.entity.Group;
public interface GroupService {

    GroupResponse createGroupWithImage(CreateGroupRequest request);


    GroupDetailResponse getGroupDetail(Integer groupId);


    GroupResponse updateGroupInformation(Integer groupId, UpdateGroupRequest request);


    List<UserJoinedGroupResponse> getUserJoinedGroups();


    GroupResponse archiveGroup(Integer groupId);
}