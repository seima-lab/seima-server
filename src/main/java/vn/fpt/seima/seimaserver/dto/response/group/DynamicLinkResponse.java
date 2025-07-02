package vn.fpt.seima.seimaserver.dto.response.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicLinkResponse {
    
    private Integer groupId;
    private String groupName;
    private String inviteCode;
    
    // Firebase Dynamic Link URLs
    private String shortLink;
    private String longLink;
    private String previewLink;
    
    // React Native Android app deep link: seimaapp://group/join/abc123
    private String deepLinkUrl;
    
    // Fallback URLs
    private String webFallbackUrl;
    private String androidFallbackUrl;
    
    // Social meta tags cho preview
    private String socialTitle;
    private String socialDescription;
    private String socialImageUrl;
    
    // Status
    private boolean success;
    private String message;
    private String errorCode;
    private LocalDateTime createdAt;
} 