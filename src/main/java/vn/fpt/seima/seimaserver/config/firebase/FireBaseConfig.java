package vn.fpt.seima.seimaserver.config.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
// THÊM IMPORT NÀY
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
// THÊM IMPORT NÀY
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
// YÊU CẦU SPRING CHẠY CLASS NÀY TRƯỚC KHI NÓ TỰ ĐỘNG CẤU HÌNH BẢO MẬT
@AutoConfigureBefore(SecurityAutoConfiguration.class)
public class FireBaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FireBaseConfig.class);

    @Value("${firebase.credential-url}")
    private String credentialUrl;

    @Value("${firebase.project-id}")
    private String projectId;

    @PostConstruct
    public void initializeFirebase() {
        logger.info("================== FIREBASE INITIALIZATION START ==================");

        try {
            // 1. Log các giá trị được inject từ file properties
            logger.info("[STEP 1] Reading properties...");
            logger.info(" > firebase.credential-url: '{}'", credentialUrl);
            logger.info(" > firebase.project-id: '{}'", projectId);

            if (projectId == null || projectId.trim().isEmpty() || projectId.equals("your-firebase-project-id")) {
                logger.error("!!!!!! CRITICAL: Project ID is null, empty, or hasn't been changed from the default. Please check your application.properties. !!!!!!");
                return; // Dừng lại nếu Project ID không hợp lệ
            }

            // 2. Tìm và kiểm tra file service account
            logger.info("[STEP 2] Locating Firebase credentials file...");
            Resource resource = new ClassPathResource(credentialUrl);
            if (!resource.exists()) {
                logger.error("!!!!!! CRITICAL: Credential file not found at path: '{}'. Make sure the file is in 'src/main/resources'. !!!!!!", credentialUrl);
                return; // Dừng lại nếu không tìm thấy file
            }
            logger.info(" > Credential file found successfully at: {}", resource.getURL());

            // 3. Đọc file và tạo credentials
            logger.info("[STEP 3] Reading credentials stream...");
            InputStream serviceAccount = resource.getInputStream();
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            logger.info(" > Credentials created successfully.");

            // 4. Xây dựng FirebaseOptions
            logger.info("[STEP 4] Building FirebaseOptions...");
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build();
            logger.info(" > FirebaseOptions built for project: {}", options.getProjectId());

            // 5. Khởi tạo FirebaseApp
            logger.info("[STEP 5] Initializing FirebaseApp...");
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                logger.info("<<<<< SUCCESS: FirebaseApp has been initialized successfully! >>>>>");
            } else {
                logger.warn(">>>>> WARN: FirebaseApp was already initialized. Skipping. <<<<<");
            }

        } catch (Exception e) {
            // Bắt tất cả các loại lỗi để không bỏ sót bất cứ điều gì
            logger.error("!!!!!! FATAL: An unexpected error occurred during Firebase initialization. !!!!!!", e);
        }

        logger.info("================== FIREBASE INITIALIZATION END ==================");
    }
}