package vn.fpt.seima.seimaserver.service;

public interface VerificationTokenService {
    
    /**
     * Generate a verification token after successful OTP verification
     * @param email the user's email
     * @return verification token
     */
    String generateVerificationToken(String email);
    
    /**
     * Validate verification token and extract email
     * @param verificationToken the token to validate
     * @return email if token is valid, null if invalid
     */
    String validateAndExtractEmail(String verificationToken);
    
    /**
     * Check if verification token is valid
     * @param verificationToken the token to check
     * @return true if valid, false otherwise
     */
    boolean isTokenValid(String verificationToken);
    
    /**
     * Invalidate a verification token after use
     * @param verificationToken the token to invalidate
     */
    void invalidateToken(String verificationToken);
} 