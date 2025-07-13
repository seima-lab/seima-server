package vn.fpt.seima.seimaserver.repository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    boolean existsByDeviceId(@NotBlank(message = "Device ID is required") @Size(max = 255, message = "Device ID must not exceed 255 characters") String deviceId);
    
    Optional<UserDevice> findByDeviceId(@NotBlank(message = "Device ID is required") @Size(max = 255, message = "Device ID must not exceed 255 characters") String deviceId);

    /**
     * Lấy danh sách FCM tokens của users để gửi notification
     * @param userIds List ID của users
     * @return List FCM tokens
     */
    @Query("SELECT ud.fcmToken FROM UserDevice ud " +
            "WHERE ud.user.userId IN :userIds " +
            "AND ud.fcmToken IS NOT NULL " +
            "AND ud.user.userIsActive = true")
    List<String> findFcmTokensByUserIds(@Param("userIds") List<Integer> userIds);
}