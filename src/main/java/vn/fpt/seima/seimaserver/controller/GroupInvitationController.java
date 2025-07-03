package vn.fpt.seima.seimaserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.group.EmailInvitationRequest;
import vn.fpt.seima.seimaserver.dto.response.group.EmailInvitationResponse;
import vn.fpt.seima.seimaserver.service.GroupInvitationService;

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
    

    @PostMapping("/groups/invitations/email")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<EmailInvitationResponse> sendEmailInvitation(@RequestBody @Valid EmailInvitationRequest request) {
        
        log.info("Received request to send email invitation for group: {} to email: {}", 
                request.getGroupId(), request.getEmail());
        
        try {
            EmailInvitationResponse response = groupInvitationService.sendEmailInvitation(request);
            
            if (response.isUserExists()) {
                return new ApiResponse<>(
                    HttpStatus.OK.value(),
                    response.getMessage(),
                    response
                );
            } else {
                return new ApiResponse<>(
                    HttpStatus.NOT_FOUND.value(),
                    response.getMessage(),
                    response
                );
            }
            
        } catch (Exception e) {
            log.error("Error sending email invitation: ", e);
            return new ApiResponse<>(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Failed to send invitation: " + e.getMessage(),
                null
            );
        }
    }
    

} 