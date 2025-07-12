package vn.fpt.seima.seimaserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.UserDevice;
import vn.fpt.seima.seimaserver.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Integer> {
    
    // Tìm device theo user và device_id
    Optional<UserDevice> findByUserAndDeviceId(User user, String deviceId);
    
    // Tìm device theo user_id và device_id
    Optional<UserDevice> findByUserUserIdAndDeviceId(Integer userId, String deviceId);
    
    // Tìm tất cả device của một user
    List<UserDevice> findByUser(User user);
    
    // Tìm tất cả device của một user theo user_id
    List<UserDevice> findByUserUserId(Integer userId);
    
    // Tìm device theo FCM token
    Optional<UserDevice> findByFcmToken(String fcmToken);
    
    // Tìm tất cả FCM token của một user
    @Query("SELECT ud.fcmToken FROM UserDevice ud WHERE ud.user.userId = :userId")
    List<String> findFcmTokensByUserId(@Param("userId") Integer userId);
    
    // Xóa device theo device_id và user_id
    void deleteByUserUserIdAndDeviceId(Integer userId, String deviceId);
    
    // Đếm số device của một user
    long countByUserUserId(Integer userId);
    
    // Tìm device theo device_id
    Optional<UserDevice> findByDeviceId(String deviceId);
    
    // Kiểm tra device có tồn tại không
    boolean existsByUserUserIdAndDeviceId(Integer userId, String deviceId);
    
    // Kiểm tra FCM token có tồn tại không
    boolean existsByFcmToken(String fcmToken);
} 