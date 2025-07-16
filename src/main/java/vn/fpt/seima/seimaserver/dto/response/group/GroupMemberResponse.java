package vn.fpt.seima.seimaserver.dto.response.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.fpt.seima.seimaserver.entity.GroupMemberRole;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupMemberResponse {
    private Integer userId;
    private String userFullName;
    private String userAvatarUrl;
    private GroupMemberRole role;
} 