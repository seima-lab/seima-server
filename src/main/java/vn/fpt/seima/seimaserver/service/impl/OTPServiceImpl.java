package vn.fpt.seima.seimaserver.service.impl;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.PhoneNumberAlreadyExistsException;
import vn.fpt.seima.seimaserver.exception.RateLimitExceededException;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.OTPService;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@AllArgsConstructor
public class OTPServiceImpl implements OTPService {
    private static final Logger logger = LoggerFactory.getLogger(OTPServiceImpl.class);
    private final UserRepository userRepository;
    private final Map<String, Bucket> phoneNumberBuckets = new ConcurrentHashMap<>();
    private final

    @Override
    public void generateOtpAndSendOtp(String rawPhoneNumber) {
        // 1. Rate Limiting
        Bucket phoneBucket = phoneNumberBuckets.computeIfAbsent(rawPhoneNumber, this::newBucketForPhoneNumber);
        if (!phoneBucket.tryConsume(1)) {
            logger.warn("Rate limit exceeded for raw phone: {}", rawPhoneNumber);
            throw new RateLimitExceededException("Too many OTP requests for this phone number. Please try again later.");
        }
        // 2. Chuẩn hóa số điện thoại
        String standardizedPhoneNumber = standardizePhoneNumber(rawPhoneNumber);
        if (standardizedPhoneNumber == null) {
            logger.warn("Invalid phone number format after standardization for raw phone: {}", rawPhoneNumber);
            throw new IllegalArgumentException("Invalid phone number format."); // Thông báo chung hơn
        }
        // 3. Kiểm tra số điện thoại đã tồn tại
        if (userRepository.findByUserPhoneNumber(standardizedPhoneNumber) != null) {
            logger.warn("Attempt to request OTP for existing standardized phone: {}", standardizedPhoneNumber);
            throw new PhoneNumberAlreadyExistsException("This phone number is already registered.");
        }
        // TODO: Lưu OTP (standardizedPhoneNumber, otp, expiryTime)

        // TODO: Gửi OTP qua SMS Gateway
        logger.info("Logic to store and send OTP for {} would be here.", standardizedPhoneNumber);

    }
    private Bucket newBucketForPhoneNumber(String phoneNumberKey) {
        Bandwidth limit = Bandwidth.classic(3, Refill.greedy(3, Duration.ofMinutes(5))); // 3 requests per 5 minutes
        return Bucket.builder().addLimit(limit).build();
    }

    private String standardizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");
        if (digitsOnly.startsWith("0") && digitsOnly.length() == 10) {
            return "+84" + digitsOnly.substring(1);
        }
        if (digitsOnly.startsWith("84") && digitsOnly.length() == 11) {
            return "+" + digitsOnly;
        }
        // Regex đã dùng trong DTO: ^(0[3|5|7|8|9])([0-9]{8})$
        // Nếu SĐT gốc (trước khi lọc) khớp regex này thì chuẩn hóa
        String vietnamesePattern = "^(0[3|5|7|8|9])([0-9]{8})$";
        if (phoneNumber.trim().matches(vietnamesePattern)) {
            return "+84" + phoneNumber.trim().substring(1);
        }
        logger.warn("Could not standardize phone number: {} to a recognized VN format.", phoneNumber);
        return null;
    }

    private String generateOtpValue() {
        java.util.Random random = new java.util.Random();
        int otpNumber = 100000 + random.nextInt(900000);
        return String.valueOf(otpNumber);
    }
}
