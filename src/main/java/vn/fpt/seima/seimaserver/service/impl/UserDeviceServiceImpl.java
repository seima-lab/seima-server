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
import vn.fpt.seima.seimaserver.service.UserService;

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
    private final UserService userService;
    
    @Override
    public UserDevice createDevice(Integer userId, String deviceId, String fcmToken) {
        log.info("Creating new device for user: {}, device: {}", userId, deviceId);
        
        // Validate input
        validateCreateDeviceInput(userId, deviceId, fcmToken);
        
        // Find user
        User user = userService.findUserById(userId);
        
        // Create new device
        UserDevice newDevice = UserDevice.builder()
                .user(user)
                .deviceId(deviceId.trim())
                .fcmToken(fcmToken.trim())
                .lastChange(LocalDateTime.now())
                .build();
        
        UserDevice savedDevice = userDeviceRepository.save(newDevice);
        log.info("Successfully created device with ID: {}", savedDevice.getId());
        
        return savedDevice;
    }



    @Override
    public UserDevice updateDeviceUser(Integer userId,String deviceId ,  String fcmToken) {
        log.info("Updating device ID: {} with FCM token: {}", deviceId, fcmToken);
        
        // Validate input
        if (deviceId == null) {
            throw new IllegalArgumentException("Existing device cannot be null");
        }
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            throw new IllegalArgumentException("FCM token cannot be null or empty");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        // Find UserDevice
        UserDevice existingDevice = findUserDeviceByDeviceId(deviceId);

        
        // Update device
        existingDevice.setFcmToken(fcmToken.trim());
        existingDevice.setLastChange(LocalDateTime.now());
        existingDevice.setUser(userService.findUserById(userId));
        
        UserDevice updatedDevice = userDeviceRepository.save(existingDevice);
        log.info("Successfully updated device ID: {} for FCM Token: {}", updatedDevice.getId(), fcmToken);
        
        return updatedDevice;
    }

    @Override
    public UserDevice updateUserIdToNull(String deviceId) {
        log.info("Updating device ID: {} to set user ID to null", deviceId);
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be null or empty");
        }
        UserDevice existingDevice = findUserDeviceByDeviceId(deviceId);
        existingDevice.setUser(null);
        existingDevice.setLastChange(LocalDateTime.now());
        UserDevice updatedDevice = userDeviceRepository.save(existingDevice);
        log.info("Successfully updated device ID: {} to set user ID to null", updatedDevice.getId());
        return updatedDevice;
    }

    // Helper methods for validation and common operations
    private void validateCreateDeviceInput(Integer userId, String deviceId, String fcmToken) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be null or empty");
        }
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            throw new IllegalArgumentException("FCM token cannot be null or empty");
        }
    }

    private UserDevice findUserDeviceByDeviceId(String deviceId) {
        return userDeviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));
    }
} 