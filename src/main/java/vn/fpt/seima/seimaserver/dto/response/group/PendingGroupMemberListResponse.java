package vn.fpt.seima.seimaserver.dto.response.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PendingGroupMemberListResponse {
    private Integer groupId;
    private String groupName;
    private Integer totalPendingCount;
    private List<PendingGroupMemberResponse> pendingMembers;
} 