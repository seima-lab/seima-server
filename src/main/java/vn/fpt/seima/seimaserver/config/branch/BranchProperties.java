package vn.fpt.seima.seimaserver.config.branch;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Branch.io configuration properties
 */
@Data
@Component
@ConfigurationProperties(prefix = "branch")
public class BranchProperties {
    
    // API credentials from Branch Dashboard
    private String branchKey;
    private String branchSecret;
    private String domain;

} 