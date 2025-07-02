package vn.fpt.seima.seimaserver.dto.request.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicLinkRequest {
    
    @NotNull(message = "Group ID is required")
    private Integer groupId;
    
    @NotBlank(message = "Invite code is required")
    private String inviteCode;
    
    @NotBlank(message = "Group name is required")
    private String groupName;
    
    private String groupAvatarUrl;
    private String inviterName;
    private Integer memberCount;
    
    // Deep link cho React Native Android app: seimaapp://group/join/abc123
    @Builder.Default
    private String androidPackageName = "com.seima.app";
    
    // Fallback URL khi chưa có app - APK download page
    @Builder.Default
    private String androidFallbackUrl = "https://seima.app.com/download/android";
    
    // Web fallback cho các platform khác
    @Builder.Default
    private String webFallbackUrl = "https://seima.app.com/download";
    
    // Social meta tags
    private String socialTitle;
    private String socialDescription;
    private String socialImageUrl;
} 