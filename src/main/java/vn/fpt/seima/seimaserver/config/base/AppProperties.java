package vn.fpt.seima.seimaserver.config.base;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    private Client client = new Client();
    
    @Data
    public static class Client {
        private String baseUrl;
    }
} 