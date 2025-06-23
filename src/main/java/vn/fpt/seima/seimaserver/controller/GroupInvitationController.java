package vn.fpt.seima.seimaserver.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.group.JoinGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.InvitationDetailsResponse;
import vn.fpt.seima.seimaserver.dto.response.group.JoinGroupResponse;
import vn.fpt.seima.seimaserver.service.GroupInvitationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Controller for handling group invitation operations
 * Provides endpoints for invitation details and joining groups
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Validated
public class GroupInvitationController {
    
    private final GroupInvitationService groupInvitationService;
    
    /**
     * Get invitation details by invite code
     * Endpoint: GET /api/v1/invites/{code}/details
     * 
     * @param inviteCode the invitation code
     * @return invitation details including group information
     */
    @GetMapping("/invites/{inviteCode}/details")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<InvitationDetailsResponse> getInvitationDetails(
            @PathVariable 
            @NotBlank(message = "Invite code cannot be blank")
            @Size(min = 8, max = 36, message = "Invite code must be between 8 and 36 characters")
            String inviteCode) {
        
        log.info("Received request to get invitation details for code: {}", inviteCode);
        
        InvitationDetailsResponse response = groupInvitationService.getInvitationDetails(inviteCode);
        
        if (Boolean.TRUE.equals(response.getIsValidInvitation())) {
            return new ApiResponse<>(
                HttpStatus.OK.value(), 
                "Invitation details retrieved successfully", 
                response
            );
        } else {
            return new ApiResponse<>(
                HttpStatus.NOT_FOUND.value(),
                response.getMessage(),
                response
            );
        }
    }
    
    /**
     * Join a group using invitation code
     * Endpoint: POST /api/v1/groups/join
     * 
     * @param request the join group request containing invite code
     * @return join group response with updated group information
     */
    @PostMapping("/groups/join")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<JoinGroupResponse> joinGroup(@RequestBody @Valid JoinGroupRequest request) {
        
        log.info("Received request to join group with invite code: {}", request.getInviteCode());
        
        JoinGroupResponse response = groupInvitationService.joinGroupByInviteCode(request);
        
        return new ApiResponse<>(
            HttpStatus.OK.value(),
            "Successfully joined the group",
            response
        );
    }
    
    /**
     * Validate invitation code
     * Endpoint: GET /api/v1/invites/{code}/validate
     * 
     * @param inviteCode the invitation code to validate
     * @return validation result
     */
    @GetMapping("/invites/{inviteCode}/validate")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Boolean> validateInvitation(
            @PathVariable 
            @NotBlank(message = "Invite code cannot be blank")
            @Size(min = 8, max = 36, message = "Invite code must be between 8 and 36 characters")
            String inviteCode) {
        
        log.info("Received request to validate invitation code: {}", inviteCode);
        
        boolean isValid = groupInvitationService.isInvitationValid(inviteCode);
        
        return new ApiResponse<>(
            HttpStatus.OK.value(),
            isValid ? "Invitation code is valid" : "Invitation code is invalid",
            isValid
        );
    }
} 