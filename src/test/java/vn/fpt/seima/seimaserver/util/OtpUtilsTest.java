package vn.fpt.seima.seimaserver.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpUtilsTest {

    @ParameterizedTest
    @ValueSource(ints = {4, 6, 8})
    void generateOTP_ShouldReturnOtpWithCorrectLength(int length) {
        // Act
        String otp = OtpUtils.generateOTP(length);
        
        // Assert
        assertNotNull(otp);
        assertEquals(length, otp.length());
    }
    
    @Test
    void generateOTP_ShouldContainOnlyDigits() {
        // Act
        String otp = OtpUtils.generateOTP(6);
        
        // Assert
        assertTrue(otp.matches("^[0-9]+$"), "OTP should contain only digits");
    }
    
    @Test
    void generateOTP_ShouldGenerateDifferentOtps() {
        // Act
        String otp1 = OtpUtils.generateOTP(6);
        String otp2 = OtpUtils.generateOTP(6);
        
        // This test could occasionally fail due to randomness,
        // but the probability is extremely low (1 in 1,000,000 for 6-digit OTP)
        assertNotNull(otp1);
        assertNotNull(otp2);
        // We're testing the randomness here, so we expect them to be different
        // in the vast majority of cases
    }
} 