package vn.fpt.seima.seimaserver.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupInvitationLandingResponse;
import vn.fpt.seima.seimaserver.service.GroupInvitationService;

/**
 * Web Controller for serving group invitation landing pages and downloads
 * Returns HTML pages using Thymeleaf templates
 * 
 * Consolidated endpoints:
 * - Landing pages (HTML)
 * - Download pages (HTML)
 * - Landing page API (JSON) - added for mobile/AJAX support
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@Validated
public class GroupInvitationWebController {
    
    private final GroupInvitationService groupInvitationService;
    
    /**
     * Serve group invitation landing page
     * URL: seima.app.com/invite/{inviteCode}
     * Returns: HTML page with group info and join button
     */
    @GetMapping("/invite/{inviteCode}")
    public String showInvitationLanding(@PathVariable String inviteCode, Model model) {
        log.info("Serving landing page for invite code: {}", inviteCode);
        
        try {
            // Get landing page data
            GroupInvitationLandingResponse response = groupInvitationService.getInvitationLandingPage(inviteCode);
            
            // Add data to model for Thymeleaf template
            model.addAttribute("invitation", response);
            model.addAttribute("inviteCode", inviteCode);

            if (response.isValidInvitation()) {
                // Return valid invitation template
                return "group-invitation-landing";
            } else {
                // Return invalid invitation template
                return "group-invitation-invalid";
            }
            
        } catch (Exception e) {
            log.error("Error serving landing page for invite code: {}", inviteCode, e);
            model.addAttribute("errorMessage", "Failed to load invitation details");
            return "group-invitation-error";
        }
    }
    
    /**
     * Download page for Android APK
     * URL: seima.app.com/download/android
     */
    @GetMapping("/download/android")
    public String showAndroidDownload(Model model) {
        log.info("Serving Android download page");
        
        model.addAttribute("appName", "Seima");
        model.addAttribute("downloadUrl", "https://github.com/your-repo/releases/latest/seima.apk");
        
        return "android-download";
    }
    
    /**
     * General download page
     * URL: seima.app.com/download
     */
    @GetMapping("/download")
    public String showGeneralDownload(Model model) {
        log.info("Serving general download page");
        
        model.addAttribute("appName", "Seima");
        model.addAttribute("androidDownloadUrl", "https://github.com/your-repo/releases/latest/seima.apk");
        model.addAttribute("playStoreUrl", "https://play.google.com/store/apps/details?id=com.seima.app");
        
        return "general-download";
    }
    
    /**
     * API endpoint for landing page data (JSON)
     * Moved from GroupInvitationController to consolidate landing page logic
     * Used for mobile apps or AJAX calls that need JSON data
     * URL: /api/landing/{inviteCode}
     */
    @GetMapping("/api/landing/{inviteCode}")
    @ResponseBody
    public ResponseEntity<ApiResponse<GroupInvitationLandingResponse>> getLandingPageData(
            @PathVariable 
            @NotBlank(message = "Invite code cannot be blank")
            @Size(min = 8, max = 36, message = "Invite code must be between 8 and 36 characters")
            String inviteCode) {
        
        log.info("API request for landing page data with invite code: {}", inviteCode);
        
        try {
            GroupInvitationLandingResponse response = groupInvitationService.getInvitationLandingPage(inviteCode);
            
            if (response.isValidInvitation()) {
                return ResponseEntity.ok(new ApiResponse<>(
                    HttpStatus.OK.value(),
                    "Landing page data retrieved successfully",
                    response
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(
                        HttpStatus.NOT_FOUND.value(),
                        response.getMessage(),
                        response
                    ));
            }
            
        } catch (Exception e) {
            log.error("Error getting landing page data for invite code: {}", inviteCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to load invitation details: " + e.getMessage(),
                    null
                ));
        }
    }
} 