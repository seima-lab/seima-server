package vn.fpt.seima.seimaserver.config.ocr;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "azure.form")
public class AzureFormRecognizerConfig {
    private String endpoint;
    private String apiKey;
}
