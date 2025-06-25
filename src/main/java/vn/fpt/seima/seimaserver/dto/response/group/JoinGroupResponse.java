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
public class JoinGroupResponse {
    private Integer groupId;
    private String groupName;
    private String groupAvatarUrl;
    private LocalDateTime joinedDate;
    private Integer memberCount;
    private String message;
} 