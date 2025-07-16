package vn.fpt.seima.seimaserver.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.group.UpdateMemberRoleRequest;
import vn.fpt.seima.seimaserver.dto.request.group.TransferOwnershipRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberListResponse;
import vn.fpt.seima.seimaserver.dto.response.group.OwnerExitOptionsResponse;
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

    @PostMapping("/group/{groupId}/exit")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> exitGroup(@PathVariable Integer groupId) {
        groupMemberService.exitGroup(groupId);
        return new ApiResponse<>(HttpStatus.OK.value(), "Successfully exited the group", null);
    }

    @PostMapping("/group/{groupId}/transfer-ownership")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> transferOwnership(
            @PathVariable Integer groupId,
            @Valid @RequestBody TransferOwnershipRequest request) {
        groupMemberService.transferOwnership(groupId, request);
        return new ApiResponse<>(HttpStatus.OK.value(), "Ownership transferred successfully", null);
    }

    @GetMapping("/group/{groupId}/eligible-for-ownership")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<GroupMemberListResponse> getEligibleMembersForOwnership(@PathVariable Integer groupId) {
        GroupMemberListResponse memberList = groupMemberService.getEligibleMembersForOwnership(groupId);
        return new ApiResponse<>(HttpStatus.OK.value(), "Eligible members for ownership retrieved successfully", memberList);
    }

    @GetMapping("/group/{groupId}/owner-exit-options")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<OwnerExitOptionsResponse> getOwnerExitOptions(@PathVariable Integer groupId) {
        OwnerExitOptionsResponse options = groupMemberService.getOwnerExitOptions(groupId);
        return new ApiResponse<>(HttpStatus.OK.value(), "Owner exit options retrieved successfully", options);
    }
} 