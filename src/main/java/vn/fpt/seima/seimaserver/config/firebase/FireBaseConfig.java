package vn.fpt.seima.seimaserver.config.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
@AutoConfigureBefore(SecurityAutoConfiguration.class)
public class FireBaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FireBaseConfig.class);

    @Bean
    @Profile("dev")
    @ConditionalOnResource(resources = "classpath:${firebase.credential-url}")
    public FirebaseApp firebaseAppDev(
            @Value("${firebase.credential-url}") String credentialUrl,
            @Value("${firebase.project-id}") String projectId) throws IOException {

        logger.info("DEV profile active. Initializing Firebase from local file: {}", credentialUrl);
        ClassPathResource resource = new ClassPathResource(credentialUrl);
        return initializeFirebaseApp(resource.getInputStream(), projectId);
    }

    @Bean
    @Profile("prod")
    @ConditionalOnProperty(name = "FCM_CREDENTIALS_JSON")
    @ConditionalOnProperty(name = "FIREBASE_PROJECT_ID")
    public FirebaseApp firebaseAppProd(
            @Value("${FCM_CREDENTIALS_JSON}") String credentialsJson,
            @Value("${FIREBASE_PROJECT_ID}") String projectId) throws IOException {

        logger.info("PROD profile active. Initializing Firebase from environment variable.");
        InputStream serviceAccountStream = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
        return initializeFirebaseApp(serviceAccountStream, projectId);
    }

    private FirebaseApp initializeFirebaseApp(InputStream serviceAccount, String projectId) throws IOException {
        if (!StringUtils.hasText(projectId)) {
            logger.error("Firebase project ID is not configured. Skipping initialization.");
            // Trả về null để bean không được tạo, tránh lỗi nếu các dependency khác là optional
            return null;
        }

        if (FirebaseApp.getApps().isEmpty()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build();

            FirebaseApp initializedApp = FirebaseApp.initializeApp(options);
            logger.info("FirebaseApp initialized successfully for project: {}", projectId);
            return initializedApp;
        } else {
            logger.warn("FirebaseApp was already initialized. Returning existing instance.");
            return FirebaseApp.getInstance();
        }
    }
}
