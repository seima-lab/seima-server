package vn.fpt.seima.seimaserver.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.fpt.seima.seimaserver.config.branch.BranchProperties;
import vn.fpt.seima.seimaserver.dto.response.group.BranchLinkResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberResponse;
import vn.fpt.seima.seimaserver.entity.Group;
import vn.fpt.seima.seimaserver.service.BranchLinkService;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of Branch.io Link Service
 * Uses Branch REST API to create deep links
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BranchLinkServiceImpl implements BranchLinkService {

    private final RestTemplate branchRestTemplate;
    private final BranchProperties branchProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${app.client.baseUrl}")
    private String appBaseUrl;

    @Override
    public BranchLinkResponse createBranchLink(Group group, GroupMemberResponse leaderResponse) {
        log.info("Creating simple Branch link for group: {} with invite code: {}", 
                group.getGroupId(), group.getGroupInviteCode());
        
        try {
            // Build simple Branch link payload
            Map<String, Object> payload = buildSimpleBranchPayload(group, leaderResponse);
            
            // Call Branch.io API
            String shortLink = callBranchAPI(payload);
            
            return BranchLinkResponse.builder()
                    .url(shortLink)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to create Branch link for group: {}", group.getGroupId(), e);
            // Fallback to web link
            String fallbackUrl = String.format("%s/invite/%s", appBaseUrl, group.getGroupInviteCode());
            return BranchLinkResponse.builder()
                    .url(fallbackUrl)
                    .build();
        }
    }

    @Override
    public BranchLinkResponse createInvitationDeepLink(Integer groupId, Integer invitedUserId, Integer inviterId, String actionType) {
        log.info("Creating invitation deep link for group: {} - inviter: {} - invited: {} - action: {}", 
                groupId, inviterId, invitedUserId, actionType);
        
        try {
            // Build invitation deep link payload with specified action
            Map<String, Object> payload = buildInvitationDeepLinkPayload(groupId, invitedUserId, inviterId, actionType);
            
            // Call Branch.io API
            String shortLink = callBranchAPI(payload);
            
            return BranchLinkResponse.builder()
                    .url(shortLink)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to create invitation deep link for group: {}", groupId, e);
            // Fallback to web link
            String fallbackUrl = String.format("%s/groups/%s", appBaseUrl, groupId);
            return BranchLinkResponse.builder()
                    .url(fallbackUrl)
                    .build();
        }
    }

    // Private helper methods

    /**
     * Build simple Branch link payload for group invitation
     */
    private Map<String, Object> buildSimpleBranchPayload(Group group, GroupMemberResponse leader) {
        Map<String, Object> payload = new HashMap<>();
        
        // Required fields
        payload.put("branch_key", branchProperties.getBranchKey());
        payload.put("groupId", group.getGroupId());
        payload.put("inviteCode", group.getGroupInviteCode());
        payload.put("groupName", group.getGroupName());
        return payload;
    }

    /**
     * Build invitation deep link payload with specified action
     * Format: { "action": "{actionType}", "groupId": "..." , invitedUserId, inviterId}
     */
    private Map<String, Object> buildInvitationDeepLinkPayload(Integer groupId, Integer invitedUserId, Integer inviterId, String actionType) {
        Map<String, Object> payload = new HashMap<>();
        
        // Required Branch.io fields
        payload.put("branch_key", branchProperties.getBranchKey());
        
        // Deep link data with specified action format
        payload.put("action", actionType);
        payload.put("groupId", groupId.toString());
        
        // Add user IDs only if provided (for RECHECK_PENDING_STATUS might not need them)
        if (invitedUserId != null) {
            payload.put("invitedUserId", invitedUserId);
        }
        if (inviterId != null) {
            payload.put("inviterId", inviterId);
        }
        
        // Deep link configuration
        payload.put("$desktop_url", String.format("%s/groups/%s", appBaseUrl, groupId));
        payload.put("$fallback_url", String.format("%s/groups/%s", appBaseUrl, groupId));
        
        // Deep link for mobile app
        payload.put("$deeplink_path", String.format("group/%s", groupId));
        payload.put("$android_deeplink_path", String.format("seimaapp://group/view/%s", groupId));
        payload.put("$ios_deeplink_path", String.format("seimaapp://group/view/%s", groupId));
        
        log.info("Built invitation deep link payload: action={}, groupId={}, invitedUserId={}, inviterId={}", 
                actionType, groupId, invitedUserId, inviterId);
        
        return payload;
    }

    /**
     * Call Branch.io REST API
     */
    private String callBranchAPI(Map<String, Object> payload) throws Exception {
        String url = "https://api2.branch.io/v1/url";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        
        try {
            ResponseEntity<String> response = branchRestTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                String shortLink = jsonResponse.path("url").asText();
                
                if (shortLink != null && !shortLink.isEmpty()) {
                    log.info("Successfully created Branch link: {}", shortLink);
                    return shortLink;
                } else {
                    throw new RuntimeException("Empty URL in response");
                }
            } else {
                throw new RuntimeException("Branch API returned status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Failed to call Branch API", e);
            throw new Exception("Branch API call failed", e);
        }
    }
} 