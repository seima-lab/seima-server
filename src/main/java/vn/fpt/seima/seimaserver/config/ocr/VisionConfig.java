package vn.fpt.seima.seimaserver.config.ocr;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Component
public class VisionConfig {

    public static void initGoogleCredentials() throws IOException {
        if (System.getProperty("GOOGLE_APPLICATION_CREDENTIALS") != null) return;

        try (InputStream in = VisionConfig.class.getResourceAsStream("/credentials/my-service-account.json")) {
            File temp = File.createTempFile("gcp-key", ".json");
            Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", temp.getAbsolutePath());
        }
    }
}
