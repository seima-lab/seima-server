package vn.fpt.seima.seimaserver.config.googlecloud;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class SpeechClientConfig {
    Logger logger = LoggerFactory.getLogger(SpeechClientConfig.class);

    @Value("${google.cloud.credentials.location}")
    private String credentialsLocation;
    @Bean
    public SpeechClient speechClient() throws IOException {

        Resource classPathResource = new ClassPathResource(credentialsLocation);
        if(!classPathResource.exists()) {
            logger.error("Google Cloud credentials file not found at path: '{}'. Please check your application.properties.", credentialsLocation);
        } else {
            logger.info("Google Cloud credentials file found successfully at: {}", classPathResource.getURL());
        }
        GoogleCredentials credentials = GoogleCredentials.fromStream(classPathResource.getInputStream());
        CredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);

        SpeechSettings speechSettings = SpeechSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider)
                .build();
        return SpeechClient.create(speechSettings);
    }
}

