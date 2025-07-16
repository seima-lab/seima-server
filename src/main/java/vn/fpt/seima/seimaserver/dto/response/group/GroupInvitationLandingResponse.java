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
public class GroupInvitationLandingResponse {
    
    // Group information
    private Integer groupId;
    private String groupName;
    private String groupAvatarUrl;
    private String groupDescription;
    private LocalDateTime groupCreatedDate;
    private Integer memberCount;
    
    // Group leader info
    private GroupMemberResponse groupLeader;
    
    // Invitation details
    private String inviteCode;
    private String inviterName;
    private String invitationMessage;
    private String invitedEmail;
    // Dynamic link for "Join Group" button
    private String joinButtonLink;

    // Sample property with simple enum
    private ResultType resultType;
    
    // App download info
    private String appDownloadUrl;
    private String appName;
    
    // Web page meta data
    private String pageTitle;
    private String pageDescription;
    private String ogImageUrl;
    
    // Status
    private boolean isValidInvitation;
    private String message;
} 