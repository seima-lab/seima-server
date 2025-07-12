package vn.fpt.seima.seimaserver.mapper;

import org.springframework.stereotype.Component;
import vn.fpt.seima.seimaserver.dto.response.device.UserDeviceResponse;
import vn.fpt.seima.seimaserver.entity.UserDevice;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserDeviceMapper {
    
    public UserDeviceResponse toResponse(UserDevice userDevice) {
        if (userDevice == null) {
            return null;
        }
        
        return UserDeviceResponse.builder()
                .id(userDevice.getId())
                .userId(userDevice.getUser().getUserId())
                .deviceId(userDevice.getDeviceId())
                .fcmToken(userDevice.getFcmToken())
                .lastLogin(userDevice.getLastLogin())
                .createdAt(userDevice.getCreatedAt())
                .updatedAt(userDevice.getUpdatedAt())
                .build();
    }
    
    public List<UserDeviceResponse> toResponseList(List<UserDevice> userDevices) {
        if (userDevices == null || userDevices.isEmpty()) {
            return List.of();
        }
        
        return userDevices.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
} 