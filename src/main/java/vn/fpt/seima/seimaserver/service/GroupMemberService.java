package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.request.group.AcceptGroupMemberRequest;
import vn.fpt.seima.seimaserver.dto.request.group.RejectGroupMemberRequest;
import vn.fpt.seima.seimaserver.dto.request.group.UpdateMemberRoleRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberListResponse;
import vn.fpt.seima.seimaserver.dto.response.group.PendingGroupMemberListResponse;
import vn.fpt.seima.seimaserver.exception.GroupException;


public interface GroupMemberService {
    

    GroupMemberListResponse getActiveGroupMembers(Integer groupId);
    PendingGroupMemberListResponse getPendingGroupMembers(Integer groupId);
    void acceptGroupMemberRequest(Integer groupId, AcceptGroupMemberRequest request);
    void rejectGroupMemberRequest(Integer groupId, RejectGroupMemberRequest request);
    void removeMemberFromGroup(Integer groupId, Integer memberUserId);
    void handleUserAccountDeactivation(Integer userId);
    void updateMemberRole(Integer groupId, Integer memberUserId, UpdateMemberRoleRequest request);
} 