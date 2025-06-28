package vn.fpt.seima.seimaserver.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.group.UpdateMemberRoleRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberListResponse;
import vn.fpt.seima.seimaserver.service.GroupMemberService;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/group-members")
public class GroupMemberController {
    
    private final GroupMemberService groupMemberService;


    @GetMapping("/group/{groupId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<GroupMemberListResponse> getActiveGroupMembers(@PathVariable Integer groupId) {
        GroupMemberListResponse memberList = groupMemberService.getActiveGroupMembers(groupId);
        return new ApiResponse<>(HttpStatus.OK.value(), "Active group members retrieved successfully", memberList);
    }

   
    @DeleteMapping("/group/{groupId}/members/{memberUserId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> removeMemberFromGroup(
            @PathVariable Integer groupId,
            @PathVariable Integer memberUserId) {
        groupMemberService.removeMemberFromGroup(groupId, memberUserId);
        return new ApiResponse<>(HttpStatus.OK.value(), "Member removed from group successfully", null);
    }
    
    @PutMapping("/group/{groupId}/members/{memberUserId}/role")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> updateMemberRole(
            @PathVariable Integer groupId,
            @PathVariable Integer memberUserId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        groupMemberService.updateMemberRole(groupId, memberUserId, request);
        return new ApiResponse<>(HttpStatus.OK.value(), "Member role updated successfully", null);
    }
} 