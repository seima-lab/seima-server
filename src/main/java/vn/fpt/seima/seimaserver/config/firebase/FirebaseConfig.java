package vn.fpt.seima.seimaserver.config.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.service-account-file:datn-25c1e-firebase-adminsdk-fbsvc-d36df1db70.json}")
    private String serviceAccountFile;

    @PostConstruct
    public void validateConfiguration() {
        log.info("Firebase configuration initialized with service account file: {}", serviceAccountFile);
    }

    @Bean
    public FirebaseApp initializeFirebase() throws IOException {
        try {
            // Kiểm tra xem FirebaseApp đã được khởi tạo chưa
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("Firebase app already initialized, returning existing instance");
                return FirebaseApp.getInstance();
            }

            log.info("Initializing Firebase with service account file: {}", serviceAccountFile);
            
            // Lấy file service account từ classpath
            ClassPathResource resource = new ClassPathResource(serviceAccountFile);
            if (!resource.exists()) {
                throw new RuntimeException("Firebase service account file not found: " + serviceAccountFile);
            }

            try (InputStream serviceAccount = resource.getInputStream()) {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp app = FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully with app name: {}", app.getName());
                return app;
            }

        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage(), e);
            throw new RuntimeException("Firebase initialization failed", e);
        } catch (Exception e) {
            log.error("Unexpected error during Firebase initialization: {}", e.getMessage(), e);
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }
}
