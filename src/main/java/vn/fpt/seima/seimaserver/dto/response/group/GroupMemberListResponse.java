package vn.fpt.seima.seimaserver.dto.response.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.fpt.seima.seimaserver.entity.GroupMemberRole;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupMemberListResponse {
    private Integer groupId;
    private String groupName;
    private String groupAvatarUrl;
    private Integer totalMembersCount;
    private GroupMemberResponse groupLeader;
    private List<GroupMemberResponse> members;
    private GroupMemberRole currentUserRole;
} 