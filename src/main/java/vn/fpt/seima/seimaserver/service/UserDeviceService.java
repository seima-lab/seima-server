package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.entity.UserDevice;
import vn.fpt.seima.seimaserver.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserDeviceService {
    
    // Đăng ký hoặc cập nhật device
    UserDevice registerOrUpdateDevice(Integer userId, String deviceId, String fcmToken);
    
    // Tìm device theo user và device_id
    Optional<UserDevice> findByUserAndDeviceId(Integer userId, String deviceId);
    
    // Tìm tất cả device của một user
    List<UserDevice> findDevicesByUserId(Integer userId);
    
    // Tìm tất cả FCM token của một user
    List<String> findFcmTokensByUserId(Integer userId);
    
    // Cập nhật FCM token
    UserDevice updateFcmToken(Integer userId, String deviceId, String newFcmToken);
    
    // Cập nhật last login
    UserDevice updateLastLogin(Integer userId, String deviceId);
    
    // Xóa device
    void removeDevice(Integer userId, String deviceId);
    
    // Xóa tất cả device của một user
    void removeAllDevicesForUser(Integer userId);
    
    // Kiểm tra device có tồn tại không
    boolean existsByUserAndDeviceId(Integer userId, String deviceId);
    
    // Đếm số device của một user
    long countDevicesByUserId(Integer userId);
    
    // Tìm device theo FCM token
    Optional<UserDevice> findByFcmToken(String fcmToken);
    
    // Xóa device theo FCM token
    void removeDeviceByFcmToken(String fcmToken);
} 