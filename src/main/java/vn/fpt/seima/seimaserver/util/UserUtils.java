package vn.fpt.seima.seimaserver.util;

import jakarta.annotation.PostConstruct; // THÊM IMPORT NÀY
import org.springframework.beans.factory.annotation.Autowired; // THÊM IMPORT NÀY
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.repository.UserRepository;

@Component

public class UserUtils {

    // Trường static để các phương thức static có thể truy cập
    private static UserRepository staticUserRepository;

    // Trường instance để Spring inject UserRepository vào
    private final UserRepository instanceUserRepository;

    // Constructor để Spring inject UserRepository
    @Autowired
    public UserUtils(UserRepository userRepository) {
        this.instanceUserRepository = userRepository;
    }


    @PostConstruct
    private void initStaticUserRepository() {

        UserUtils.staticUserRepository = this.instanceUserRepository;
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal() == null || "anonymousUser".equals(authentication.getPrincipal().toString())) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        String userIdentifier;

        if (principal instanceof UserDetails) {
            userIdentifier = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            userIdentifier = (String) principal;
        } else if (principal instanceof vn.fpt.seima.seimaserver.entity.User) {
            // Trường hợp principal chính là User entity của bạn
            return (vn.fpt.seima.seimaserver.entity.User) principal;
        } else {
            System.err.println("Unexpected principal type in UserUtils: " + principal.getClass().getName());
            return null;
        }

        if (userIdentifier == null) {
            return null;
        }

        // Kiểm tra xem staticUserRepository đã được khởi tạo chưa (đề phòng)
        if (staticUserRepository == null) {
            // Điều này không nên xảy ra nếu @PostConstruct hoạt động đúng
            // và UserUtils được quản lý bởi Spring.
            System.err.println("FATAL: staticUserRepository in UserUtils is null. Spring context might not be fully initialized or UserUtils is used outside of Spring management.");
            throw new IllegalStateException("UserRepository not initialized in UserUtils. Static field is null.");
        }

        // Bây giờ staticUserRepository sẽ không còn null
        return staticUserRepository.findByUserEmail(userIdentifier).orElse(null);
    }
}