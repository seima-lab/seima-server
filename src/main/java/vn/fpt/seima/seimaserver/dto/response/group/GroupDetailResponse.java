package vn.fpt.seima.seimaserver.dto.response.group;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.fpt.seima.seimaserver.entity.GroupMemberRole;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupDetailResponse {
    private Integer groupId;
    private String groupName;
    private String groupInviteLink;
    private String groupAvatarUrl;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime groupCreatedDate;
    
    private Boolean groupIsActive;
    private GroupMemberResponse groupLeader;
    private List<GroupMemberResponse> members;
    private Integer totalMembersCount;
    private GroupMemberRole currentUserRole;
} 