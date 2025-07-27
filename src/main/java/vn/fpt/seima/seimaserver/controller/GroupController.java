package vn.fpt.seima.seimaserver.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.request.group.UpdateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupDetailResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupResponse;
import vn.fpt.seima.seimaserver.dto.response.group.UserJoinedGroupResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberStatusResponse;
import vn.fpt.seima.seimaserver.dto.response.group.UserPendingGroupResponse;
import vn.fpt.seima.seimaserver.service.GroupService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups")
public class GroupController {
    
    private final GroupService groupService;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupResponse> createGroup(@ModelAttribute @Validated CreateGroupRequest request) {
        GroupResponse groupResponse = groupService.createGroupWithImage(request);
        return new ApiResponse<>(HttpStatus.CREATED.value(), "Group created successfully", groupResponse);
    }

    @GetMapping("/{groupId}")
    @ResponseStatus(HttpStatus.OK)       
    public ApiResponse<GroupDetailResponse> getGroupDetail(@PathVariable Integer groupId) {
        GroupDetailResponse groupDetail = groupService.getGroupDetail(groupId);
        return new ApiResponse<>(HttpStatus.OK.value(), "Group detail retrieved successfully", groupDetail);
    }

    @PutMapping(value = "/{groupId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<GroupResponse> updateGroupInformation(
            @PathVariable Integer groupId,
            @ModelAttribute @Validated UpdateGroupRequest request) {
        GroupResponse groupResponse = groupService.updateGroupInformation(groupId, request);
        return new ApiResponse<>(HttpStatus.OK.value(), "Group information updated successfully", groupResponse);
    }

    @GetMapping("/joined")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<UserJoinedGroupResponse>> getUserJoinedGroups() {
        List<UserJoinedGroupResponse> joinedGroups = groupService.getUserJoinedGroups();
        return new ApiResponse<>(HttpStatus.OK.value(), "User joined groups retrieved successfully", joinedGroups);
    }

    @GetMapping("/pending")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<UserPendingGroupResponse>> getUserPendingGroups() {
        log.info("Request to get user pending groups");
        List<UserPendingGroupResponse> pendingGroups = groupService.getUserPendingGroups();
        return new ApiResponse<>(HttpStatus.OK.value(), "User pending groups retrieved successfully", pendingGroups);
    }

    @DeleteMapping("/{groupId}/archive")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<GroupResponse> archiveGroup(@PathVariable Integer groupId) {
        GroupResponse groupResponse = groupService.archiveGroup(groupId);
        return new ApiResponse<>(HttpStatus.OK.value(), "Group archived successfully", groupResponse);
    }


    @GetMapping("/{groupId}/my-status")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<GroupMemberStatusResponse> getMyGroupStatus(@PathVariable Integer groupId) {
        log.info("Request to get user status for group ID: {}", groupId);
        
        GroupMemberStatusResponse statusResponse = groupService.getCurrentUserGroupStatus(groupId);
        
        return new ApiResponse<>(
            HttpStatus.OK.value(),
            "User group status retrieved successfully",
            statusResponse
        );
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> deleteGroup(@PathVariable Integer groupId) {
        groupService.deleteGroup(groupId);
        return new ApiResponse<>(HttpStatus.OK.value(), "Group deleted successfully", null);
    }
} 