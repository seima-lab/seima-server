package vn.fpt.seima.seimaserver.dto.response.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OwnerExitOptionsResponse {
    
    private Integer groupId;
    private String groupName;
    private boolean canTransferOwnership;
    private boolean canDeleteGroup;
    private int eligibleMembersCount;
    private String message;
} 