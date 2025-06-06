package vn.fpt.seima.seimaserver.service.impl;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import vn.fpt.seima.seimaserver.dto.request.auth.NormalRegisterRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.OtpValueDto;
import vn.fpt.seima.seimaserver.dto.request.auth.VerifyOtpRequestDto;
import vn.fpt.seima.seimaserver.dto.response.auth.NormalRegisterResponseDto;
import vn.fpt.seima.seimaserver.exception.GmailAlreadyExistException;
import vn.fpt.seima.seimaserver.exception.InvalidOtpException;
import vn.fpt.seima.seimaserver.exception.MaxOtpAttemptsExceededException;
import vn.fpt.seima.seimaserver.exception.NullRequestParamException;
import vn.fpt.seima.seimaserver.exception.OtpNotFoundException;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.AuthService;
import vn.fpt.seima.seimaserver.service.EmailService;
import vn.fpt.seima.seimaserver.util.OtpUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.core.RedisTemplate;

@Service
public class AuthServiceImpl implements AuthService {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${app.lab-name}")
    private String labName;

    @Value("${app.email.otp-register.html-template}")
    private String otpRegisterHtmlTemplate;

    @Value("${redis.host}")
    private String redisHost;
    @Value("${redis.port}")
    private String redisPort;

    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final long OTP_EXPIRATION_TIME = 5; // in minutes
    private static final int MAX_INCORRECT_OTP_ATTEMPTS = 3;

    private final Map<String,Bucket> rateLimitBuckets = new ConcurrentHashMap<>();
    private static final String OTP_KEY_PREFIX = "otp-:";

    @Override
    public void logout(HttpServletRequest request) {

    }


    @Override
    @Transactional
    public NormalRegisterResponseDto processRegister(NormalRegisterRequestDto normalRegisterRequestDto) {

        // 0. Rate Limiting check can be added here if needed
        Bucket registerBucket = rateLimitBuckets.computeIfAbsent(
            normalRegisterRequestDto.getUserName(),
            key -> newBucketForRegister()
        );
        
        if (!registerBucket.tryConsume(1)) {
            throw new RuntimeException("Rate limit exceeded. Please try again later.");
        }

        if(normalRegisterRequestDto.getUserName() == null || normalRegisterRequestDto.getPassword() == null) {
            throw new NullRequestParamException("Username and password must not be null");
         }
         // 1.Here to check email exist in system
        if(userRepository.findByUserEmail(normalRegisterRequestDto.getUserName()).isPresent()) {
            throw new GmailAlreadyExistException("Email already exists in the system");
        }

        // 2. Generate OTP
        String otp = OtpUtils.generateOTP(6);
        // 3.Process send OTP to mail
        Context context = new Context();
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("userName", normalRegisterRequestDto.getUserName());
        variables.put("otpRegister", otp);
        variables.put("appName",labName);
        context.setVariables(variables);
        String emailCurrentUser = normalRegisterRequestDto.getUserName();
        String emailSubject ="Xác thực tài khoản Seima";
        String templateName = otpRegisterHtmlTemplate;
        emailService.sendEmailWithHtmlTemplate(emailCurrentUser, emailSubject, templateName, context);
        // 4. Save OTP to Redis
        String otpKey = OTP_KEY_PREFIX + emailCurrentUser;
        
        // Check if the OTP already exists for this email
        OtpValueDto existingOtp = (OtpValueDto) redisTemplate.opsForValue().get(otpKey);
        
        if (existingOtp != null) {
            // If the user has reached max attempts, throw exception
            if (existingOtp.getAttemptCount() >= MAX_OTP_ATTEMPTS) {
                throw new MaxOtpAttemptsExceededException("Maximum OTP attempts exceeded. Please try again later.");
            }
            
            // Increment attempt count
            OtpValueDto otpValueDto = OtpValueDto.builder()
                    .otpCode(otp)
                    .attemptCount(existingOtp.getAttemptCount() + 1)
                    .build();
            
            // Save to Redis with expiration
            redisTemplate.opsForValue().set(otpKey, otpValueDto);
            redisTemplate.expire(otpKey, Duration.ofMinutes(OTP_EXPIRATION_TIME));
        } else {
            // Create new OTP entry
            OtpValueDto otpValueDto = OtpValueDto.builder()
                    .otpCode(otp)
                    .attemptCount(1)
                    .build();
            
            // Save to Redis with expiration
            redisTemplate.opsForValue().set(otpKey, otpValueDto);
            redisTemplate.expire(otpKey, Duration.ofMinutes(OTP_EXPIRATION_TIME));
        }

        return NormalRegisterResponseDto.builder()
                .email(emailCurrentUser)
                .otpCode(otp)
                .build();
    }

    @Override
    @Transactional
    public boolean verifyOtp(VerifyOtpRequestDto verifyOtpRequestDto) {
        if (verifyOtpRequestDto.getEmail() == null || verifyOtpRequestDto.getOtp() == null) {
            throw new NullRequestParamException("Email and OTP must not be null");
        }
        
        String email = verifyOtpRequestDto.getEmail();
        String otp = verifyOtpRequestDto.getOtp();
        String password = verifyOtpRequestDto.getPassword();
        
        // Get OTP from Redis
        String otpKey = OTP_KEY_PREFIX + email;
        OtpValueDto otpValueDto = (OtpValueDto) redisTemplate.opsForValue().get(otpKey);
        
        // Check if OTP exists
        if (otpValueDto == null) {
            throw new OtpNotFoundException("OTP not found or expired. Please request a new OTP.");
        }
        
        // Check if OTP is correct
        if (!otpValueDto.getOtpCode().equals(otp)) {
            // Increment incorrect attempts counter
            int incorrectAttempts = otpValueDto.getIncorrectAttempts() != null ? 
                    otpValueDto.getIncorrectAttempts() + 1 : 1;
                    
            // Check if max incorrect attempts reached
            if (incorrectAttempts >= MAX_INCORRECT_OTP_ATTEMPTS) {
                // Delete OTP from Redis
                redisTemplate.delete(otpKey);
                throw new MaxOtpAttemptsExceededException("Maximum incorrect OTP attempts reached. Please request a new OTP.");
            }
            
            // Update incorrect attempts in Redis
            otpValueDto.setIncorrectAttempts(incorrectAttempts);
            redisTemplate.opsForValue().set(otpKey, otpValueDto);
            redisTemplate.expire(otpKey, Duration.ofMinutes(OTP_EXPIRATION_TIME));
            
            throw new InvalidOtpException("Invalid OTP. Attempts remaining: " + (MAX_INCORRECT_OTP_ATTEMPTS - incorrectAttempts));
        }
        
        // OTP verification successful
        // Delete OTP from Redis
        redisTemplate.delete(otpKey);
        
        // Create and save user
        User user = User.builder()
                .userEmail(email)
                .userIsActive(true)
                .userCreatedDate(LocalDateTime.now())
                .build();
        
        userRepository.save(user);
        
        return true;
    }
    
    @Override
    public void resendOtp(String email) {
        if (email == null) {
            throw new NullRequestParamException("Email must not be null");
        }
        
        // Rate limiting check
        Bucket resendBucket = rateLimitBuckets.computeIfAbsent(
            email + ":resend",
            key -> newBucketForResend()
        );
        
        if (!resendBucket.tryConsume(1)) {
            throw new RuntimeException("Rate limit exceeded for OTP resend. Please try again later.");
        }
        
        // Check if user exists
        if (userRepository.findByUserEmail(email).isPresent()) {
            throw new GmailAlreadyExistException("Email already exists in the system");
        }
        
        // Check if previous OTP exists and update attempt count
        String otpKey = OTP_KEY_PREFIX + email;
        OtpValueDto existingOtp = (OtpValueDto) redisTemplate.opsForValue().get(otpKey);
        
        // Generate new OTP
        String otp = OtpUtils.generateOTP(6);
        
        // Send OTP email
        Context context = new Context();
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("userName", email);
        variables.put("otpRegister", otp);
        variables.put("appName", labName);
        context.setVariables(variables);
        
        String emailSubject = "Xác thực tài khoản Seima - Gửi lại mã OTP";
        String templateName = otpRegisterHtmlTemplate;
        emailService.sendEmailWithHtmlTemplate(email, emailSubject, templateName, context);
        
        // Save new OTP to Redis with incremented attempt count if applicable
        int attemptCount = existingOtp != null ? existingOtp.getAttemptCount() + 1 : 1;
        
        if (attemptCount > MAX_OTP_ATTEMPTS) {
            throw new MaxOtpAttemptsExceededException("Maximum OTP attempts exceeded. Please try again later.");
        }
        
        OtpValueDto otpValueDto = OtpValueDto.builder()
                .otpCode(otp)
                .attemptCount(attemptCount)
                .incorrectAttempts(0)
                .build();
        
        redisTemplate.opsForValue().set(otpKey, otpValueDto);
        redisTemplate.expire(otpKey, Duration.ofMinutes(OTP_EXPIRATION_TIME));
    }
    
    private Bucket newBucketForRegister() {
        Bandwidth limit = Bandwidth.classic(3, Refill.greedy(3, Duration.ofMinutes(5))); // 3 requests per 5 minutes
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
    
    private Bucket newBucketForResend() {
        Bandwidth limit = Bandwidth.classic(2, Refill.greedy(2, Duration.ofMinutes(10))); // 2 requests per 10 minutes
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
