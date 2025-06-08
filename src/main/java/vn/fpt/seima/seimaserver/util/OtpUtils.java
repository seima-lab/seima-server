package vn.fpt.seima.seimaserver.util;

import org.springframework.stereotype.Component;

@Component
public class OtpUtils {
    public static String generateOTP(int length) {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int digit = (int) (Math.random() * 10); // Generate a random digit from 0 to 9
            otp.append(digit);
        }
        return otp.toString();
    }
}
