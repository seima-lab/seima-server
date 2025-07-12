package vn.fpt.seima.seimaserver.config.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FireBaseConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(FireBaseConfig.class);
    
    @Value("${firebase.credential-url}")
    private String firebaseCredentialUrl;

    @PostConstruct
    public void initializeFirebase() {
        try {
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                try (FileInputStream serviceAccount = new FileInputStream(firebaseCredentialUrl)) {
                    FirebaseOptions options = new FirebaseOptions.Builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();

                    FirebaseApp.initializeApp(options);
                    logger.info("Firebase initialized successfully");
                } catch (IOException e) {
                    logger.error("Failed to initialize Firebase: {}", e.getMessage());
                    throw new RuntimeException("Failed to initialize Firebase", e);
                }
            } else {
                logger.info("Firebase is already initialized");
            }
        } catch (Exception e) {
            logger.error("Error during Firebase initialization: {}", e.getMessage());
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }
}
