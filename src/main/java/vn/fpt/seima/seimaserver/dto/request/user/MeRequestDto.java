package vn.fpt.seima.seimaserver.dto.request.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MeRequestDto {
    public String fcmToken;
    public String deviceId;
}
