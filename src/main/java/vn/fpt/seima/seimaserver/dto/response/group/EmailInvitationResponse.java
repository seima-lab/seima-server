package vn.fpt.seima.seimaserver.dto.response.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailInvitationResponse {
    
    private Integer groupId;
    private String groupName;
    private String invitedEmail;
    private String inviteLink;
    private String message;
    private boolean emailSent;
    private boolean userExists;
} 