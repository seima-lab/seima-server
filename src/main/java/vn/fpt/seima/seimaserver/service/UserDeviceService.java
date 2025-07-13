package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.entity.UserDevice;
import vn.fpt.seima.seimaserver.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserDeviceService {
    
    /**
     * Create new device record
     */
    UserDevice createDevice(Integer userId, String deviceId, String fcmToken);
    
    /**
     * Update existing device with new user and fcm token
     */
    UserDevice updateDeviceUser(String deviceId, String fcmToken);
    

    UserDevice updateUserIdToNull(String deviceId);
    
} 