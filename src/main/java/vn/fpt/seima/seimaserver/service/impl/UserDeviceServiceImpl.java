package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.entity.UserDevice;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.repository.UserDeviceRepository;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.UserDeviceService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserDeviceServiceImpl implements UserDeviceService {
    
    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;
    
    @Override
    public UserDevice registerOrUpdateDevice(Integer userId, String deviceId, String fcmToken) {
        log.info("Registering or updating device for user: {}, device: {}", userId, deviceId);
        
        // Tìm user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Tìm device đã tồn tại
        Optional<UserDevice> existingDevice = userDeviceRepository.findByUserUserIdAndDeviceId(userId, deviceId);
        
        if (existingDevice.isPresent()) {
            // Cập nhật device hiện tại
            UserDevice device = existingDevice.get();
            device.setFcmToken(fcmToken);
            device.setLastLogin(LocalDateTime.now());
            log.info("Updated existing device for user: {}, device: {}", userId, deviceId);
            return userDeviceRepository.save(device);
        } else {
            // Tạo device mới
            UserDevice newDevice = UserDevice.builder()
                    .user(user)
                    .deviceId(deviceId)
                    .fcmToken(fcmToken)
                    .lastLogin(LocalDateTime.now())
                    .build();
            log.info("Created new device for user: {}, device: {}", userId, deviceId);
            return userDeviceRepository.save(newDevice);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<UserDevice> findByUserAndDeviceId(Integer userId, String deviceId) {
        return userDeviceRepository.findByUserUserIdAndDeviceId(userId, deviceId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UserDevice> findDevicesByUserId(Integer userId) {
        return userDeviceRepository.findByUserUserId(userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<String> findFcmTokensByUserId(Integer userId) {
        return userDeviceRepository.findFcmTokensByUserId(userId);
    }
    
    @Override
    public UserDevice updateFcmToken(Integer userId, String deviceId, String newFcmToken) {
        log.info("Updating FCM token for user: {}, device: {}", userId, deviceId);
        
        UserDevice device = userDeviceRepository.findByUserUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found for user: " + userId + " and device: " + deviceId));
        
        device.setFcmToken(newFcmToken);
        return userDeviceRepository.save(device);
    }
    
    @Override
    public UserDevice updateLastLogin(Integer userId, String deviceId) {
        log.info("Updating last login for user: {}, device: {}", userId, deviceId);
        
        UserDevice device = userDeviceRepository.findByUserUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found for user: " + userId + " and device: " + deviceId));
        
        device.setLastLogin(LocalDateTime.now());
        return userDeviceRepository.save(device);
    }
    
    @Override
    public void removeDevice(Integer userId, String deviceId) {
        log.info("Removing device for user: {}, device: {}", userId, deviceId);
        
        UserDevice device = userDeviceRepository.findByUserUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found for user: " + userId + " and device: " + deviceId));
        
        userDeviceRepository.delete(device);
    }
    
    @Override
    public void removeAllDevicesForUser(Integer userId) {
        log.info("Removing all devices for user: {}", userId);
        
        List<UserDevice> devices = userDeviceRepository.findByUserUserId(userId);
        userDeviceRepository.deleteAll(devices);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserAndDeviceId(Integer userId, String deviceId) {
        return userDeviceRepository.existsByUserUserIdAndDeviceId(userId, deviceId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countDevicesByUserId(Integer userId) {
        return userDeviceRepository.countByUserUserId(userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<UserDevice> findByFcmToken(String fcmToken) {
        return userDeviceRepository.findByFcmToken(fcmToken);
    }
    
    @Override
    public void removeDeviceByFcmToken(String fcmToken) {
        log.info("Removing device by FCM token: {}", fcmToken);
        
        UserDevice device = userDeviceRepository.findByFcmToken(fcmToken)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with FCM token: " + fcmToken));
        
        userDeviceRepository.delete(device);
    }
} 