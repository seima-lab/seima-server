package vn.fpt.seima.seimaserver.dto.request.group;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferOwnershipRequest {
    
    @NotNull(message = "New owner user ID is required")
    private Integer newOwnerUserId;
} 