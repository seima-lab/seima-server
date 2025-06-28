package vn.fpt.seima.seimaserver.dto.request.group;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.fpt.seima.seimaserver.entity.GroupMemberRole;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateMemberRoleRequest {
    
    @NotNull(message = "New role is required")
    private GroupMemberRole newRole;
} 