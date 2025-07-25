package vn.fpt.seima.seimaserver.config.googlecloud;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Configuration for SpeechClient.
 * This class provides environment-aware beans for SpeechClient.
 * - In 'dev' profile, it loads credentials from a local classpath resource.
 * - In 'prod' profile, it loads credentials from an environment variable/property.
 * The creation of the bean is conditional to prevent application startup failure
 * if the required configuration is missing.
 */
@Configuration
public class SpeechClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(SpeechClientConfig.class);


    @Bean("speechClient")
    @Profile("dev")
    @ConditionalOnResource(resources = "classpath:${google.cloud.credentials.location}")
    public SpeechClient speechClientDev(@Value("${google.cloud.credentials.location}") String credentialsLocation) throws IOException {
        logger.info("DEV profile active. Attempting to create SpeechClient from classpath resource: {}", credentialsLocation);

        ClassPathResource resource = new ClassPathResource(credentialsLocation);
        GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream());
        CredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);

        SpeechSettings speechSettings = SpeechSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider)
                .build();

        SpeechClient client = SpeechClient.create(speechSettings);
        logger.info("SpeechClient created successfully for DEV profile.");
        return client;
    }


    @Bean("speechClient")
    @Profile("prod")
    @ConditionalOnProperty(name = "GOOGLE_CREDENTIALS_JSON")
    public SpeechClient speechClientProd(@Value("${GOOGLE_CREDENTIALS_JSON}") String credentialsJson) throws IOException {
        logger.info("PROD profile active. Attempting to create SpeechClient from environment variable/property.");

        InputStream serviceAccountStream = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccountStream);
        CredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);

        SpeechSettings speechSettings = SpeechSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider)
                .build();

        SpeechClient client = SpeechClient.create(speechSettings);
        logger.info("SpeechClient created successfully for PROD profile.");
        return client;
    }
}
