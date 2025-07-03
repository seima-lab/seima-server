package vn.fpt.seima.seimaserver.config.branch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

/**
 * Branch.io configuration
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class BranchConfig {

    private final BranchProperties branchProperties;

    @PostConstruct
    public void validateConfiguration() {
        log.info("Branch.io configuration initialized with domain: {}", 
                branchProperties.getDomain());
        
        if (branchProperties.getBranchKey() == null) {
            log.warn("Branch key not configured - deep linking will fail");
        }
    }

    @Bean
    public RestTemplate branchRestTemplate() {
        return new RestTemplate();
    }
} 