package vn.fpt.seima.seimaserver.service.impl;

import com.google.auth.oauth2.GoogleCredentials;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.fpt.seima.seimaserver.config.firebase.FirebaseDynamicLinksProperties;
import vn.fpt.seima.seimaserver.dto.request.group.DynamicLinkRequest;
import vn.fpt.seima.seimaserver.dto.response.group.DynamicLinkResponse;
import vn.fpt.seima.seimaserver.service.FirebaseDynamicLinkService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of Firebase Dynamic Link Service
 * Uses Firebase REST API to create dynamic links
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseDynamicLinkServiceImpl implements FirebaseDynamicLinkService {

    private final RestTemplate restTemplate;
    private final FirebaseDynamicLinksProperties firebaseProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${app.client.baseUrl}")
    private String appBaseUrl;

    @Override
    public DynamicLinkResponse createGroupJoinLink(DynamicLinkRequest request) {
        log.info("Creating Firebase Dynamic Link for group: {} with invite code: {}", 
                request.getGroupId(), request.getInviteCode());
        
        try {
            // Build deep link URL: seimaapp://group/join/{inviteCode}
            String deepLinkUrl = buildDeepLinkUrl(request.getInviteCode());
            
            // Create Dynamic Link request payload
            Map<String, Object> dynamicLinkPayload = buildDynamicLinkPayload(request, deepLinkUrl);
            
            // Call Firebase Dynamic Links API
            String shortLink = callFirebaseDynamicLinksAPI(dynamicLinkPayload);
            
            // Build response
            return DynamicLinkResponse.builder()
                    .groupId(request.getGroupId())
                    .groupName(request.getGroupName())
                    .inviteCode(request.getInviteCode())
                    .shortLink(shortLink)
                    .longLink(buildLongLink(request.getInviteCode()))
                    .deepLinkUrl(deepLinkUrl)
                    .webFallbackUrl(request.getWebFallbackUrl())
                    .androidFallbackUrl(request.getAndroidFallbackUrl())
                    .socialTitle(request.getSocialTitle())
                    .socialDescription(request.getSocialDescription())
                    .socialImageUrl(request.getSocialImageUrl())
                    .success(true)
                    .message("Dynamic link created successfully")
                    .createdAt(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to create Firebase Dynamic Link for group: {}", request.getGroupId(), e);
            return DynamicLinkResponse.builder()
                    .groupId(request.getGroupId())
                    .groupName(request.getGroupName())
                    .inviteCode(request.getInviteCode())
                    .success(false)
                    .message("Failed to create dynamic link: " + e.getMessage())
                    .errorCode("DYNAMIC_LINK_CREATION_FAILED")
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public String extractInviteCodeFromLink(String dynamicLink) {
        if (dynamicLink == null || dynamicLink.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Pattern để extract invite code từ deep link
            // Ví dụ: seimaapp://group/join/abc123 → abc123
            Pattern pattern = Pattern.compile("seimaapp://group/join/([a-zA-Z0-9-]+)");
            Matcher matcher = pattern.matcher(dynamicLink);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // Fallback: extract từ URL parameters
            if (dynamicLink.contains("inviteCode=")) {
                Pattern urlPattern = Pattern.compile("inviteCode=([a-zA-Z0-9-]+)");
                Matcher urlMatcher = urlPattern.matcher(dynamicLink);
                if (urlMatcher.find()) {
                    return urlMatcher.group(1);
                }
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Failed to extract invite code from dynamic link: {}", dynamicLink, e);
            return null;
        }
    }

    @Override
    public String validateAndExtractDeepLink(String dynamicLink) {
        if (dynamicLink == null || dynamicLink.trim().isEmpty()) {
            return null;
        }
        
        try {
                         // Validate if it's a Firebase Dynamic Link
            String domain = firebaseProperties.getDynamicLinks().getDomain();
            if (!dynamicLink.contains(domain)) {
                log.warn("Dynamic link does not contain expected domain: {}", domain);
                return null;
            }
            
            // Extract deep link from Firebase Dynamic Link
            // Có thể cần call Firebase API để resolve link, nhưng tạm thời dùng pattern matching
            String inviteCode = extractInviteCodeFromLink(dynamicLink);
            if (inviteCode != null) {
                return buildDeepLinkUrl(inviteCode);
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Failed to validate and extract deep link: {}", dynamicLink, e);
            return null;
        }
    }

    /**
     * Build deep link URL for React Native app
     * React Native Linking: seimaapp://group/join/{inviteCode}
     */
    private String buildDeepLinkUrl(String inviteCode) {
        return String.format("seimaapp://group/join/%s", inviteCode);
    }

    /**
     * Build long link URL (fallback web URL)
     */
    private String buildLongLink(String inviteCode) {
        return String.format("%s/invite/%s", appBaseUrl, inviteCode);
    }

    /**
     * Build Dynamic Link payload for Firebase API
     */
    private Map<String, Object> buildDynamicLinkPayload(DynamicLinkRequest request, String deepLinkUrl) {
        Map<String, Object> dynamicLink = new HashMap<>();
        
        // Main dynamic link object
        Map<String, Object> payload = new HashMap<>();
        payload.put("dynamicLinkInfo", dynamicLink);
        payload.put("suffix", Map.of("option", "SHORT"));
        
        // Set domain and deep link
        String domain = firebaseProperties.getDynamicLinks().getDomain();
        dynamicLink.put("domainUriPrefix", "https://" + domain);
        dynamicLink.put("link", deepLinkUrl);
        
        // Android info cho React Native
        Map<String, Object> androidInfo = new HashMap<>();
        androidInfo.put("androidPackageName", request.getAndroidPackageName());
        androidInfo.put("androidFallbackLink", request.getAndroidFallbackUrl());
        // Minimum version code cho React Native app
        androidInfo.put("androidMinPackageVersionCode", "1");
        dynamicLink.put("androidInfo", androidInfo);
        
        // Social meta tags
        if (request.getSocialTitle() != null || request.getSocialDescription() != null) {
            Map<String, Object> socialMetaTagInfo = new HashMap<>();
            if (request.getSocialTitle() != null) {
                socialMetaTagInfo.put("socialTitle", request.getSocialTitle());
            }
            if (request.getSocialDescription() != null) {
                socialMetaTagInfo.put("socialDescription", request.getSocialDescription());
            }
            if (request.getSocialImageUrl() != null) {
                socialMetaTagInfo.put("socialImageLink", request.getSocialImageUrl());
            }
            dynamicLink.put("socialMetaTagInfo", socialMetaTagInfo);
        }
        
        return payload;
    }

    /**
     * Call Firebase Dynamic Links REST API
     */
    private String callFirebaseDynamicLinksAPI(Map<String, Object> payload) throws IOException {
        String url = String.format("https://firebasedynamiclinks.googleapis.com/v1/shortLinks?key=%s", 
                firebaseProperties.getWebApiKey());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                String shortLink = jsonResponse.path("shortLink").asText();
                
                if (shortLink != null && !shortLink.isEmpty()) {
                    log.info("Successfully created Firebase Dynamic Link: {}", shortLink);
                    return shortLink;
                } else {
                    throw new RuntimeException("Empty short link in response");
                }
            } else {
                throw new RuntimeException("Firebase API returned status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Failed to call Firebase Dynamic Links API", e);
            throw new IOException("Firebase Dynamic Links API call failed", e);
        }
    }
} 