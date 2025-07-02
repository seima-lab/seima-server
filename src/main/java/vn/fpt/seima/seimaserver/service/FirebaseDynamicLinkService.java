package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.request.group.DynamicLinkRequest;
import vn.fpt.seima.seimaserver.dto.response.group.DynamicLinkResponse;

/**
 * Service interface for Firebase Dynamic Link operations
 * Handles creation and management of dynamic invitation links
 */
public interface FirebaseDynamicLinkService {
    
    /**
     * Generate Firebase Dynamic Link for group invitation "Join" button
     * Link format: seimaapp://group/join/{inviteCode}
     * 
     * @param request Dynamic link request with invitation details
     * @return DynamicLinkResponse with short and long URLs
     */
    DynamicLinkResponse createGroupJoinLink(DynamicLinkRequest request);
    
    /**
     * Extract invite code from dynamic link URL
     * @param dynamicLink The Firebase dynamic link URL
     * @return extracted invite code if valid, null otherwise
     */
    String extractInviteCodeFromLink(String dynamicLink);
    
    /**
     * Validate dynamic link format and extract deep link
     * @param dynamicLink The Firebase dynamic link URL
     * @return deep link URL if valid, null otherwise
     */
    String validateAndExtractDeepLink(String dynamicLink);
} 