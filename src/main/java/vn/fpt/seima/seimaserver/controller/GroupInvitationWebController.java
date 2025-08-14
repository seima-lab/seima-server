package vn.fpt.seima.seimaserver.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.fpt.seima.seimaserver.dto.response.group.GroupInvitationLandingResponse;
import vn.fpt.seima.seimaserver.dto.response.group.SuccessAcceptanceGroupResponse;
import vn.fpt.seima.seimaserver.service.GroupInvitationService;

/**
 * Web controller for handling group invitation landing pages with invitation tokens
 * Supports only invitation tokens, not legacy invite codes
 */
@Controller
@RequestMapping("/invite")
public class GroupInvitationWebController {

    private static final Logger logger = LoggerFactory.getLogger(GroupInvitationWebController.class);
    
    private final GroupInvitationService groupInvitationService;

    public GroupInvitationWebController(GroupInvitationService groupInvitationService) {
        this.groupInvitationService = groupInvitationService;
    }

    @GetMapping("/{inviteToken}")
    public Object handleInvitationToken(@PathVariable String inviteToken) {
        logger.info("Received invitation token access: {}", inviteToken);
        
        try {
            // Call service to process invitation token and handle business logic
            GroupInvitationLandingResponse response = groupInvitationService.processInvitationToken(inviteToken);
         
            switch (response.getResultType()) {
                // Các trường hợp này cần chuyển hướng người dùng đến deep link của Branch
                case STATUS_CHANGE_TO_PENDING_APPROVAL:
                case ALREADY_PENDING_APPROVAL:
                case ALREADY_ACTIVE_MEMBER:
                    return "redirect:" + response.getJoinButtonLink();
                case INVALID_OR_USED_TOKEN:
                    return "error_invalid_invitation";

                case GROUP_INACTIVE_OR_DELETED:
                    return "error_group_inactive";

                case GROUP_FULL:
                    return "error_group_full";

                default:
                    // Xử lý các trường hợp không mong muốn
                    logger.error("Unhandled ResultType: {}", response.getResultType());
                    return "error_generic"; // Có thể tạo một trang lỗi chung
            }
            
        } catch (Exception e) {
            logger.error("Error processing invitation token: {}", inviteToken, e);
            return "error_invalid_invitation"; // Return static error page for any unexpected error
        }
    }

    @GetMapping("/success-acceptance/group")
    public Object handleSuccessAcceptance(
            @RequestParam("userId") Long userId,
            @RequestParam("groupId") Long groupId
    ) {
        logger.info("User {} successfully accepted invitation to gr oup {}", userId, groupId);
        
        try {
            // Validate input parameters
            if (userId == null || groupId == null) {
                logger.warn("Invalid parameters: userId={}, groupId={}", userId, groupId);
                return "error_invalid_invitation";
            }

            // Call service to process success acceptance
            SuccessAcceptanceGroupResponse response = groupInvitationService.handleSuccessAcceptance(userId, groupId);

            if (response == null) {
                logger.error("Service returned null response for userId={}, groupId={}", userId, groupId);
                return "error_invalid_invitation";
            }
            
            // Handle different result types
            switch (response.getResultType()) {
                case GROUP_INACTIVE_OR_DELETED:
                    logger.warn("Group {} is inactive or deleted for user {}", groupId, userId);
                    return "error_group_inactive";
                    
                case SUCCESS:
                    if (response.getJoinButtonLink() != null && !response.getJoinButtonLink().trim().isEmpty()) {
                        logger.info("Redirecting user {} to group {} via deep link", userId, groupId);
                        return "redirect:" + response.getJoinButtonLink();
                    } else {
                        logger.error("Success response missing join button link for userId={}, groupId={}", userId, groupId);
                        return "error_invalid_invitation";
                    }

                case INVALID_OR_USED_TOKEN:
                    logger.warn("Invalid or used token for userId={}, groupId={}", userId, groupId);
                    return "error_invalid_invitation";

                case GROUP_FULL:
                    logger.warn("Group {} is full for user {}", groupId, userId);
                    return "error_group_full";

                case STATUS_CHANGE_TO_PENDING_APPROVAL:
                case ALREADY_PENDING_APPROVAL:
                case ALREADY_ACTIVE_MEMBER:
                    // These cases should not occur in success acceptance flow
                    logger.warn("Unexpected result type {} for success acceptance: userId={}, groupId={}",
                              response.getResultType(), userId, groupId);
                    return "error_invalid_invitation";
                    
                default:
                    logger.error("Unhandled ResultType: {} for userId={}, groupId={}", 
                               response.getResultType(), userId, groupId);
                    return "error_invalid_invitation";
            }
            
        } catch (Exception e) {
            logger.error("Error processing success acceptance for userId={}, groupId={}", userId, groupId, e);
            return "error_invalid_invitation";
        }
    }




} 