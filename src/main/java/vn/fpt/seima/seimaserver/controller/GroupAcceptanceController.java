package vn.fpt.seima.seimaserver.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.group.AcceptGroupMemberRequest;
import vn.fpt.seima.seimaserver.dto.request.group.RejectGroupMemberRequest;
import vn.fpt.seima.seimaserver.dto.response.group.PendingGroupMemberListResponse;
import vn.fpt.seima.seimaserver.service.GroupMemberService;

/**
 * Controller for managing group acceptance functionality
 * Handles viewing and processing pending group member requests
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/group-acceptance")
public class GroupAcceptanceController {
    
    private final GroupMemberService groupMemberService;

    /**
     * Get all pending member requests for a specific group
     * Only admins and owners can view pending requests
     * 
     * @param groupId the ID of the group
     * @return ApiResponse containing list of pending member requests
     */
    @GetMapping("/group/{groupId}/pending-requests")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PendingGroupMemberListResponse> getPendingGroupMembers(@PathVariable Integer groupId) {
        log.info("Request to get pending members for group ID: {}", groupId);
        
        PendingGroupMemberListResponse pendingMembers = groupMemberService.getPendingGroupMembers(groupId);
        
        return new ApiResponse<>(
            HttpStatus.OK.value(), 
            "Pending group member requests retrieved successfully", 
            pendingMembers
        );
    }

    /**
     * Accept a pending member request for a specific group
     * Only admins and owners can accept requests
     * 
     * @param groupId the ID of the group
     * @param request the request containing user ID to accept
     * @return ApiResponse with success message
     */
    @PostMapping("/group/{groupId}/accept-request")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> acceptGroupMemberRequest(
            @PathVariable Integer groupId,
            @Valid @RequestBody AcceptGroupMemberRequest request) {
        log.info("Request to accept member for group ID: {}, user ID: {}", groupId, request.getUserId());
        
        groupMemberService.acceptGroupMemberRequest(groupId, request);
        
        return new ApiResponse<>(
            HttpStatus.OK.value(), 
            "Group member request accepted successfully", 
            null
        );
    }

    /**
     * Reject a pending member request for a specific group
     * Only admins and owners can reject requests
     * 
     * @param groupId the ID of the group
     * @param request the request containing user ID to reject
     * @return ApiResponse with success message
     */
    @PostMapping("/group/{groupId}/reject-request")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> rejectGroupMemberRequest(
            @PathVariable Integer groupId,
            @Valid @RequestBody RejectGroupMemberRequest request) {
        log.info("Request to reject member for group ID: {}, user ID: {}", groupId, request.getUserId());
        
        groupMemberService.rejectGroupMemberRequest(groupId, request);
        
        return new ApiResponse<>(
            HttpStatus.OK.value(), 
            "Group member request rejected successfully", 
            null
        );
    }
}
