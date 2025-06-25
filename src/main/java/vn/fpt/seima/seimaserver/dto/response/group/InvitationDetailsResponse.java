package vn.fpt.seima.seimaserver.dto.response.group;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationDetailsResponse {
    private Integer groupId;
    private String groupName;
    private String groupAvatarUrl;
    private LocalDateTime groupCreatedDate;
    private Integer memberCount;
    private GroupMemberResponse groupLeader;
    private String inviteLink;
    private Boolean isValidInvitation;
    private String message; // Thông báo lỗi nếu invitation không hợp lệ
} 