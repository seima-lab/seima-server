package vn.fpt.seima.seimaserver.dto.request.group;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RejectGroupMemberRequest {
    
    @NotNull(message = "User ID cannot be null")
    private Integer userId;
} 