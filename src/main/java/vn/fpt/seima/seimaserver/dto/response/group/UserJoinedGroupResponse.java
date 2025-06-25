package vn.fpt.seima.seimaserver.dto.response.group;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import vn.fpt.seima.seimaserver.entity.GroupMemberRole;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserJoinedGroupResponse {
    private Integer groupId;
    private String groupName;
    private String groupAvatarUrl;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime groupCreatedDate;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinedDate;
    
    private GroupMemberRole userRole;
    private Integer totalMembersCount;
    private GroupMemberResponse groupLeader;
} 