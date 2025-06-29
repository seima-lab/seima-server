package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.request.group.UpdateMemberRoleRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberListResponse;
import vn.fpt.seima.seimaserver.exception.GroupException;


public interface GroupMemberService {
    

    GroupMemberListResponse getActiveGroupMembers(Integer groupId);
    void removeMemberFromGroup(Integer groupId, Integer memberUserId);
    void handleUserAccountDeactivation(Integer userId);
    void updateMemberRole(Integer groupId, Integer memberUserId, UpdateMemberRoleRequest request);
} 