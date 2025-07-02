package vn.fpt.seima.seimaserver.config.firebase;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "firebase")
public class FirebaseDynamicLinksProperties {
    
    private String serviceAccountFile;
    private String webApiKey;
    private DynamicLinks dynamicLinks = new DynamicLinks();
    
    @Data
    public static class DynamicLinks {
        private String domain = "seimaapp.page.link";
    }
} 