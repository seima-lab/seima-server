package vn.fpt.seima.seimaserver.dto.response.group;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PendingGroupMemberResponse {
    private Integer userId;
    private String userFullName;
    private String userAvatarUrl;
    private String userEmail;
    private LocalDateTime requestedAt;
} 