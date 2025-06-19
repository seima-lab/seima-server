package vn.fpt.seima.seimaserver.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.service.RedisService;
import vn.fpt.seima.seimaserver.service.VerificationTokenService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class VerificationTokenServiceImpl implements VerificationTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(VerificationTokenServiceImpl.class);
    
    @Autowired
    private RedisService redisService;
    
    private static final String VERIFICATION_TOKEN_PREFIX = "verification-token:";
    private static final long TOKEN_EXPIRATION_TIME = 15; // 15 minutes


    // This method generates a verification token after successful OTP verification
    @Override
    public String generateVerificationToken(String email) {
        String tokenId = UUID.randomUUID().toString();
        String tokenKey = VERIFICATION_TOKEN_PREFIX + tokenId;
        
        // Store email with token in Redis
        redisService.set(tokenKey, email);
        redisService.setTimeToLiveInMinutes(tokenKey, TOKEN_EXPIRATION_TIME);
        
        logger.info("Generated verification token for email: {}", email);
        return tokenId;
    }

    // This method validates the token and extracts the email if valid
    @Override
    public String validateAndExtractEmail(String verificationToken) {
        if (verificationToken == null || verificationToken.trim().isEmpty()) {
            return null;
        }
        
        String tokenKey = VERIFICATION_TOKEN_PREFIX + verificationToken;
        String email = redisService.getObject(tokenKey, String.class);
        
        if (email != null) {
            logger.info("Valid verification token for email: {}", email);
            return email;
        }
        
        logger.warn("Invalid or expired verification token: {}", verificationToken);
        return null;
    }

    // This method checks if the verification token is valid
    @Override
    public boolean isTokenValid(String verificationToken) {
        return validateAndExtractEmail(verificationToken) != null;
    }

    // This method invalidates the verification token after use
    @Override
    public void invalidateToken(String verificationToken) {
        if (verificationToken != null && !verificationToken.trim().isEmpty()) {
            String tokenKey = VERIFICATION_TOKEN_PREFIX + verificationToken;
            redisService.delete(tokenKey);
            logger.info("Invalidated verification token: {}", verificationToken);
        }
    }
} 