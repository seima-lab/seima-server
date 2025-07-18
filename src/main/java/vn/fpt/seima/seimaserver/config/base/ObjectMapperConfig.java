package vn.fpt.seima.seimaserver.config.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for ObjectMapper bean
 * Configures JSON serialization/deserialization settings
 */
@Configuration
public class ObjectMapperConfig {
    
    /**
     * Configure ObjectMapper bean for JSON operations
     * Includes JavaTimeModule for LocalDateTime serialization
     * Uses snake_case property naming strategy
     * 
     * @return configured ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Register JavaTimeModule for LocalDateTime support
        objectMapper.registerModule(new JavaTimeModule());
        
        // Configure serialization settings
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Set property naming strategy to snake_case
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        
        return objectMapper;
    }
} 