package vn.fpt.seima.seimaserver.service.impl;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import vn.fpt.seima.seimaserver.dto.request.auth.GoogleLoginRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.ForgotPasswordRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.LoginRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.NormalRegisterRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.OtpValueDto;
import vn.fpt.seima.seimaserver.dto.request.auth.ResetPasswordRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.SetNewPasswordRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.VerifyForgotPasswordOtpRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.VerifyOtpRequestDto;
import vn.fpt.seima.seimaserver.dto.response.auth.LoginResponseDto;
import vn.fpt.seima.seimaserver.dto.response.auth.NormalRegisterResponseDto;
import vn.fpt.seima.seimaserver.dto.response.auth.VerifyForgotPasswordOtpResponseDto;
import vn.fpt.seima.seimaserver.dto.response.user.UserInGoogleReponseDto;
import vn.fpt.seima.seimaserver.exception.AccountNotVerifiedException;
import vn.fpt.seima.seimaserver.exception.GmailAlreadyExistException;
import vn.fpt.seima.seimaserver.exception.GoogleAccountConflictException;
import vn.fpt.seima.seimaserver.exception.InvalidOtpException;
import vn.fpt.seima.seimaserver.exception.MaxOtpAttemptsExceededException;
import vn.fpt.seima.seimaserver.exception.NullRequestParamException;
import vn.fpt.seima.seimaserver.exception.OtpNotFoundException;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.repository.UserDeviceRepository;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.*;
import vn.fpt.seima.seimaserver.util.OtpUtils;
import vn.fpt.seima.seimaserver.dto.request.auth.ChangePasswordRequestDto;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;

@Service
public class AuthServiceImpl implements AuthService {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordValidationService passwordValidationService;
    
    @Autowired
    private VerificationTokenService verificationTokenService;

    @Autowired
    private UserDeviceService userDeviceService;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @Value("${app.lab-name}")
    private String labName;

    @Value("${app.email.otp-register.html-template}")
    private String otpRegisterHtmlTemplate;

    @Value("${app.email.password-reset.html-template}")
    private String passwordResetHtmlTemplate;

    @Value("${app.email.password-reset.subject}")
    private String passwordResetSubject;

    @Value("${redis.host}")
    private String redisHost;
    @Value("${redis.port}")
    private String redisPort;

    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final long OTP_EXPIRATION_TIME = 3; // in minutes
    private static final int MAX_INCORRECT_OTP_ATTEMPTS = 3;
    private static final long VERIFICATION_TOKEN_EXPIRATION = 15 * 60; // 15 minutes in seconds

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
            normalRegisterRequestDto.getEmail(),
            key -> newBucketForRegister()
        );
        

        // 0.1 Validate confirm password
        if (normalRegisterRequestDto.getConfirmPassword() == null ||
            !normalRegisterRequestDto.getConfirmPassword().equals(normalRegisterRequestDto.getPassword())) {
            throw new InvalidOtpException("Confirm password does not match the password");
        }

        if(normalRegisterRequestDto.getEmail().isEmpty()
                || normalRegisterRequestDto.getPassword().isEmpty()
                || normalRegisterRequestDto.getFullName().isEmpty()
                || normalRegisterRequestDto.getPhoneNumber().isEmpty()
                || normalRegisterRequestDto.getConfirmPassword().isEmpty()
                || normalRegisterRequestDto.getConfirmPassword().isEmpty()
                || normalRegisterRequestDto.getPassword().isEmpty()
        ) {
            throw new NullRequestParamException("Fields must not be null");
         }
         
         // 1. Check email exist in system
        Optional<User> existingUser = userRepository.findByUserEmail(normalRegisterRequestDto.getEmail());
        if(existingUser.isPresent()) {
            User user = existingUser.get();
            // Check if this is a Google account
            if (user.getIsLogByGoogle()) {
                throw new GmailAlreadyExistException("This email is already registered with Google login. Please use Google login instead.");
            } else if (user.getUserIsActive()) {
                // User exists and is already active
                throw new GmailAlreadyExistException("Email already exists in the system");
            }
            // User exists but is not active - update user information and allow re-registration with OTP
            user.setUserFullName(normalRegisterRequestDto.getFullName());
            user.setUserDob(normalRegisterRequestDto.getDob());
            user.setUserPhoneNumber(normalRegisterRequestDto.getPhoneNumber());
            user.setUserGender(normalRegisterRequestDto.isGender());
            user.setUserPassword(passwordEncoder.encode(normalRegisterRequestDto.getPassword()));
            user.setUserCreatedDate(LocalDateTime.now()); // Update created date
            userRepository.save(user);
        } else {
            // Create new inactive user
            User newUser = User.builder()
                    .userEmail(normalRegisterRequestDto.getEmail())
                    .userFullName(normalRegisterRequestDto.getFullName())
                    .isLogByGoogle(false)
                    .userDob(normalRegisterRequestDto.getDob())
                    .userPhoneNumber(normalRegisterRequestDto.getPhoneNumber())
                    .userGender(normalRegisterRequestDto.isGender())
                    .userPassword(passwordEncoder.encode(normalRegisterRequestDto.getPassword()))
                    .userIsActive(false) // User starts as inactive
                    .userCreatedDate(LocalDateTime.now())
                    .build();
            
            userRepository.save(newUser);
        }

        // 2. Generate OTP
        String otp = OtpUtils.generateOTP(6);
        // 3.Process send OTP to mail
        logger.warn("is sending mail to: " + normalRegisterRequestDto.getEmail());
        Context context = new Context();
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("userName", normalRegisterRequestDto.getEmail());
        variables.put("otpRegister", otp);
        variables.put("appName",labName);
        context.setVariables(variables);
        String emailCurrentUser = normalRegisterRequestDto.getEmail();
        String emailSubject ="Xác thực tài khoản Seima";
        String templateName = otpRegisterHtmlTemplate;
        emailService.sendEmailWithHtmlTemplate(emailCurrentUser, emailSubject, templateName, context);
        // 4. Save OTP to Redis
        String otpKey = OTP_KEY_PREFIX + emailCurrentUser;
        logger.info("sending OTP to email success: " + emailCurrentUser);
        // Check if the OTP already exists for this email
        OtpValueDto existingOtp = redisService.getObject(otpKey, OtpValueDto.class);
        
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
            redisService.set(otpKey,otpValueDto);
            redisService.setTimeToLiveInMinutes(otpKey, OTP_EXPIRATION_TIME);
        } else {
            // Create new OTP entry
            OtpValueDto otpValueDto = OtpValueDto.builder()
                    .otpCode(otp)
                    .attemptCount(1)
                    .build();
            
            // Save to Redis with expiration
            redisService.set(otpKey,otpValueDto);
            redisService.setTimeToLiveInMinutes(otpKey, OTP_EXPIRATION_TIME);
        }

        return NormalRegisterResponseDto.builder()
                .email(emailCurrentUser)
                .phoneNumber(normalRegisterRequestDto.getPhoneNumber())
                .fullName(normalRegisterRequestDto.getFullName())
                .dob(normalRegisterRequestDto.getDob())
                .gender(normalRegisterRequestDto.isGender())
                .password(normalRegisterRequestDto.getPassword())
                .otpCode(otp)
                .build();
    }

    @Override
    @Transactional
    public boolean verifyOtp(VerifyOtpRequestDto verifyOtpRequestDto) {
        if (verifyOtpRequestDto.getEmail() == null || verifyOtpRequestDto.getOtp().isEmpty()) {
            throw new NullRequestParamException("Email and OTP must not be null");
        }
        
        String email = verifyOtpRequestDto.getEmail();
        String otp = verifyOtpRequestDto.getOtp();
        
        // Get OTP from Redis
        String otpKey = OTP_KEY_PREFIX + email;
        OtpValueDto otpValueDto = redisService.getObject(otpKey, OtpValueDto.class);
        
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
                redisService.delete(otpKey);
                throw new MaxOtpAttemptsExceededException("Maximum incorrect OTP attempts reached. Please request a new OTP.");
            }
            
            // Update incorrect attempts in Redis
            otpValueDto.setIncorrectAttempts(incorrectAttempts);
            redisService.set(otpKey, otpValueDto);
            redisService.setTimeToLiveInMinutes(otpKey, OTP_EXPIRATION_TIME);

            
            throw new InvalidOtpException("Invalid OTP. Attempts remaining: " + (MAX_INCORRECT_OTP_ATTEMPTS - incorrectAttempts));
        }
        
        // OTP verification successful
        // Delete OTP from Redis
        redisService.delete(otpKey);
        
        // Find and activate existing user
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new NullRequestParamException("User not found"));
        
        // Update user information if needed and activate account
        if (verifyOtpRequestDto.getFullName() != null) {
            user.setUserFullName(verifyOtpRequestDto.getFullName());
        }
        if (verifyOtpRequestDto.getDob() != null) {
            user.setUserDob(verifyOtpRequestDto.getDob());
        }
        if (verifyOtpRequestDto.getPhoneNumber() != null) {
            user.setUserPhoneNumber(verifyOtpRequestDto.getPhoneNumber());
        }
        user.setUserGender(verifyOtpRequestDto.isGender());
        user.setUserIsActive(true); // Activate the user
        
        userRepository.save(user);
        
        return true;
    }
    
    @Override
    public void resendOtp(String email) {
        // Check if user exists
        Optional<User> userOptional = userRepository.findByUserEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getUserIsActive()) {
                throw new GmailAlreadyExistException("Email already exists and is active in the system");
            }
            // User exists but not active - allow resend OTP
        } else {
            throw new NullRequestParamException("User not found. Please register first.");
        }

        String otpKey = OTP_KEY_PREFIX + email;

        // Check if OTP already exists in Redis
        OtpValueDto existingOtpValueDto = redisService.getObject(otpKey, OtpValueDto.class);

        int attemptCount = 1;
        if (existingOtpValueDto != null) {
            attemptCount = existingOtpValueDto.getAttemptCount() + 1;
        }

        // Check if max attempts exceeded
        if (attemptCount > MAX_OTP_ATTEMPTS) {
            throw new MaxOtpAttemptsExceededException("Maximum OTP attempts exceeded. Please try again later.");
        }

        // Generate new OTP
        String otp = OtpUtils.generateOTP(6);

        // Send email
        Context context = new Context();
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("userName", email);
        variables.put("otpRegister", otp);
        variables.put("appName", labName);
        context.setVariables(variables);
        emailService.sendEmailWithHtmlTemplate(email, "Seima - OTP Resend", otpRegisterHtmlTemplate, context);

        // Save new OTP to Redis
        OtpValueDto otpValueDto = OtpValueDto.builder()
                .otpCode(otp)
                .attemptCount(attemptCount)
                .incorrectAttempts(0)
                .build();
        redisService.set(otpKey, otpValueDto);
        redisService.setTimeToLiveInMinutes(otpKey, OTP_EXPIRATION_TIME);
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

    @Override
    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        String email = loginRequestDto.getEmail();
        String password = loginRequestDto.getPassword();

        // Find user by email
        Optional<User> userOptional = userRepository.findByUserEmail(email);
        
        if (userOptional.isEmpty()) {
            throw new InvalidOtpException("Invalid email or password");
        }
        
        User user = userOptional.get();

        // Check if user is inactive
        if (!user.getUserIsActive()) {
            String otpKey = OTP_KEY_PREFIX + email;
            OtpValueDto existingOtp = redisService.getObject(otpKey, OtpValueDto.class);
            
            if (existingOtp != null) {
                // Còn OTP → Báo cần verify
                throw new AccountNotVerifiedException("Your account is not verified. Please check your email and verify your account with the OTP code.");
            } else {
                // Hết OTP → Giả vờ user không tồn tại
                throw new InvalidOtpException("Invalid email or password");
            }
        }

        // Check if user has password (not Google account)
        if (user.getUserPassword() == null) {
            throw new GoogleAccountConflictException("This account was created with Google login. Please use Google login.");
        }

        // Verify password
        if (!passwordEncoder.matches(password, user.getUserPassword())) {
            throw new InvalidOtpException("Invalid email or password");
        }

        // Create user DTO for JWT
        UserInGoogleReponseDto userDto = UserInGoogleReponseDto.builder()
                .email(user.getUserEmail())
                .fullName(user.getUserFullName())
                .avatarUrl(user.getUserAvatarUrl())
                .build();

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(userDto);
        String refreshToken = jwtService.generateRefreshToken(userDto);

        // Lưu data vào bảng user_device
        if(userDeviceRepository.existsByDeviceId(loginRequestDto.getDeviceId())) {
            userDeviceService.updateDeviceUser(loginRequestDto.getDeviceId(), loginRequestDto.getFcmToken());
        }
        userDeviceService.createDevice(user.getUserId(),loginRequestDto.getDeviceId(),loginRequestDto.getFcmToken());

        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userInformation(userDto)
                .message("Login successful")
                .build();
    }

    @Override
    public void forgotPassword(ForgotPasswordRequestDto forgotPasswordRequestDto) {
        String email = forgotPasswordRequestDto.getEmail();
        
        // Check if user exists
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new NullRequestParamException("User with email " + email + " not found"));
        
        // Check if this is a Google account
        if (user.getIsLogByGoogle()) {
            throw new GoogleAccountConflictException("This account was created with Google login. Password reset is not available for Google accounts. Please use Google login.");
        }
        
        // Check if user has password (additional validation)
        if (user.getUserPassword() == null) {
            throw new GoogleAccountConflictException("This account does not have a password set. Please use Google login.");
        }
        
        // Generate OTP
        String otp = OtpUtils.generateOTP(6);
        
        // Save OTP to Redis with forgot password prefix
        String otpKey = "forgot-password-otp:" + email;
        
        // Check if there's an existing OTP attempt
        OtpValueDto existingOtp = redisService.getObject(otpKey, OtpValueDto.class);
        int attemptCount = 1;
        
        if (existingOtp != null) {
            attemptCount = existingOtp.getAttemptCount() + 1;
            
            // Check if max attempts exceeded
            if (attemptCount > MAX_OTP_ATTEMPTS) {
                throw new MaxOtpAttemptsExceededException("Maximum OTP attempts exceeded. Please try again later.");
            }
        }
        
        OtpValueDto otpValueDto = OtpValueDto.builder()
                .otpCode(otp)
                .attemptCount(attemptCount)
                .incorrectAttempts(0)
                .build();
        
        redisService.set(otpKey, otpValueDto);
        redisService.setTimeToLiveInMinutes(otpKey, OTP_EXPIRATION_TIME);
        
        // Send OTP email
        try {
            Context context = new Context();
            HashMap<String, Object> variables = new HashMap<>();
            variables.put("otp", otp);
            variables.put("labName", labName);
            variables.put("userName", user.getUserFullName());
            context.setVariables(variables);
            
            emailService.sendEmailWithHtmlTemplate(
                    email,
                    passwordResetSubject,
                    passwordResetHtmlTemplate,
                    context
            );
            
            logger.info("Forgot password OTP sent successfully to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to send forgot password OTP email to: {}", email, e);
            // Delete the OTP from Redis if email sending fails
            redisService.delete(otpKey);
            throw new RuntimeException("Failed to send OTP email");
        }
    }
    
    @Override
    @Transactional
    public boolean resetPassword(ResetPasswordRequestDto resetPasswordRequestDto) {
        String email = resetPasswordRequestDto.getEmail();
        String otp = resetPasswordRequestDto.getOtp();
        String newPassword = resetPasswordRequestDto.getNewPassword();
        
        // Validate input
        if (email == null || otp == null || newPassword == null) {
            throw new NullRequestParamException("Email, OTP, and new password are required");
        }
        
        // Check if user exists and validate account type
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new NullRequestParamException("User not found"));
        
        // Check if this is a Google account
        if (user.getIsLogByGoogle()) {
            throw new GoogleAccountConflictException("This account was created with Google login. Password reset is not available for Google accounts.");
        }
        
        // Verify OTP
        String otpKey = "forgot-password-otp:" + email;
        OtpValueDto otpValueDto = redisService.getObject(otpKey, OtpValueDto.class);
        
        if (otpValueDto == null) {
            throw new OtpNotFoundException("OTP not found or expired");
        }
        
        if (!otpValueDto.getOtpCode().equals(otp)) {
            // Increment incorrect attempts
            int incorrectAttempts = otpValueDto.getIncorrectAttempts() != null ? 
                    otpValueDto.getIncorrectAttempts() + 1 : 1;
            
            if (incorrectAttempts >= MAX_INCORRECT_OTP_ATTEMPTS) {
                redisService.delete(otpKey);
                throw new MaxOtpAttemptsExceededException("Maximum OTP verification attempts exceeded. Please request a new OTP.");
            }
            
            // Update incorrect attempts in Redis
            otpValueDto.setIncorrectAttempts(incorrectAttempts);
            redisService.set(otpKey, otpValueDto);
            redisService.setTimeToLiveInMinutes(otpKey, OTP_EXPIRATION_TIME);
            
            throw new InvalidOtpException("Invalid OTP. Attempts remaining: " + (MAX_INCORRECT_OTP_ATTEMPTS - incorrectAttempts));
        }
        
        // Update user password
        user.setUserPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Clean up OTP from Redis
        redisService.delete(otpKey);
        
        logger.info("Password reset successful for user: {}", email);
        return true;
    }

    @Override
    public void resendForgotPasswordOtp(String email) {
        // Check if user exists
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new NullRequestParamException("User with email " + email + " not found"));
        
        // Check if this is a Google account
        if (user.getIsLogByGoogle()) {
            throw new GoogleAccountConflictException("This account was created with Google login. Password reset is not available for Google accounts. Please use Google login.");
        }
        
        // Check if user has password (additional validation)
        if (user.getUserPassword() == null) {
            throw new GoogleAccountConflictException("This account does not have a password set. Please use Google login.");
        }
        
        String otpKey = "forgot-password-otp:" + email;
        
        // Check if OTP already exists in Redis
        OtpValueDto existingOtpValueDto = redisService.getObject(otpKey, OtpValueDto.class);
        
        int attemptCount = 1;
        if (existingOtpValueDto != null) {
            attemptCount = existingOtpValueDto.getAttemptCount() + 1;
        }
        
        // Check if max attempts exceeded
        if (attemptCount > MAX_OTP_ATTEMPTS) {
            throw new MaxOtpAttemptsExceededException("Maximum OTP attempts exceeded. Please try again later.");
        }
        
        // Generate new OTP
        String otp = OtpUtils.generateOTP(6);
        
        // Create new OTP entry
        OtpValueDto otpValueDto = OtpValueDto.builder()
                .otpCode(otp)
                .attemptCount(attemptCount)
                .incorrectAttempts(0)
                .build();
        
        // Save to Redis with expiration
        redisService.set(otpKey, otpValueDto);
        redisService.setTimeToLiveInMinutes(otpKey, OTP_EXPIRATION_TIME);
        
        // Send email
        try {
            Context context = new Context();
            HashMap<String, Object> variables = new HashMap<>();
            variables.put("otp", otp);
            variables.put("labName", labName);
            variables.put("userName", user.getUserFullName());
            context.setVariables(variables);
            
            emailService.sendEmailWithHtmlTemplate(
                    email, 
                    passwordResetSubject + " - Resend", 
                    passwordResetHtmlTemplate, 
                    context
            );
            
            logger.info("Forgot password OTP resent successfully to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to resend forgot password OTP email to: {}", email, e);
            // Delete the OTP from Redis if email sending fails
            redisService.delete(otpKey);
            throw new RuntimeException("Failed to resend OTP email");
        }
    }

    @Override
    @Transactional
    public VerifyForgotPasswordOtpResponseDto verifyForgotPasswordOtp(VerifyForgotPasswordOtpRequestDto verifyForgotPasswordOtpRequestDto) {
        String email = verifyForgotPasswordOtpRequestDto.getEmail();
        String otp = verifyForgotPasswordOtpRequestDto.getOtp();
        
        // Validate input
        if (email == null || otp == null) {
            throw new NullRequestParamException("Email and OTP are required");
        }
        
        // Check if user exists and validate account type
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new NullRequestParamException("User not found"));
        
        // Check if this is a Google account
        if (user.getIsLogByGoogle()) {
            throw new GoogleAccountConflictException("This account was created with Google login. Password reset is not available for Google accounts.");
        }
        
        // Verify OTP
        String otpKey = "forgot-password-otp:" + email;
        OtpValueDto otpValueDto = redisService.getObject(otpKey, OtpValueDto.class);
        
        if (otpValueDto == null) {
            throw new OtpNotFoundException("OTP not found or expired");
        }
        
        if (!otpValueDto.getOtpCode().equals(otp)) {
            // Increment incorrect attempts
            int incorrectAttempts = otpValueDto.getIncorrectAttempts() != null ? 
                    otpValueDto.getIncorrectAttempts() + 1 : 1;
            
            if (incorrectAttempts >= MAX_INCORRECT_OTP_ATTEMPTS) {
                redisService.delete(otpKey);
                throw new MaxOtpAttemptsExceededException("Maximum OTP verification attempts exceeded. Please request a new OTP.");
            }
            
            // Update incorrect attempts in Redis
            otpValueDto.setIncorrectAttempts(incorrectAttempts);
            redisService.set(otpKey, otpValueDto);
            redisService.setTimeToLiveInMinutes(otpKey, OTP_EXPIRATION_TIME);
            
            throw new InvalidOtpException("Invalid OTP. Attempts remaining: " + (MAX_INCORRECT_OTP_ATTEMPTS - incorrectAttempts));
        }
        
        // OTP verification successful - generate verification token
        String verificationToken = verificationTokenService.generateVerificationToken(email);
        
        // Clean up OTP from Redis since it's verified
        redisService.delete(otpKey);
        
        logger.info("OTP verification successful for user: {}", email);
        
        return VerifyForgotPasswordOtpResponseDto.builder()
                .email(email)
                .verified(true)
                .verificationToken(verificationToken)
                .expiresIn(VERIFICATION_TOKEN_EXPIRATION)
                .build();
    }
    
    @Override
    @Transactional
    public boolean setNewPasswordAfterVerification(SetNewPasswordRequestDto setNewPasswordRequestDto, String verificationToken) {
        String email = setNewPasswordRequestDto.getEmail();
        String newPassword = setNewPasswordRequestDto.getNewPassword();
        
        // Validate input
        if (email == null || newPassword == null || verificationToken == null) {
            throw new NullRequestParamException("Email, new password, and verification token are required");
        }
        
        // Validate verification token and extract email
        String tokenEmail = verificationTokenService.validateAndExtractEmail(verificationToken);
        if (tokenEmail == null) {
            throw new InvalidOtpException("Invalid or expired verification token");
        }
        
        // Ensure the email in the request matches the token
        if (!email.equals(tokenEmail)) {
            throw new InvalidOtpException("Email mismatch with verification token");
        }
        
        // Find user
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new NullRequestParamException("User not found"));
        
        // Check if this is a Google account (double check)
        if (user.getIsLogByGoogle()) {
            throw new GoogleAccountConflictException("This account was created with Google login. Password reset is not available for Google accounts.");
        }
        
        // Update user password
        user.setUserPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Invalidate the verification token
        verificationTokenService.invalidateToken(verificationToken);
        
        logger.info("Password reset successful for user: {}", email);
        return true;
    }

    @Override
    @Transactional
    public boolean changePassword(String userEmail, ChangePasswordRequestDto changePasswordRequestDto) {
        logger.info("Starting password change process for user: {}", userEmail);
        
        // 1. Validate request
        passwordValidationService.validateChangePasswordRequest(changePasswordRequestDto);
        
        // 2. Find user
        User user = userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new NullRequestParamException("User not found with email: " + userEmail));
        
        // 3. Validate user can change password
        passwordValidationService.validateUserCanChangePassword(user);
        
        // 4. Validate old password
        passwordValidationService.validateOldPassword(user, changePasswordRequestDto.getOldPassword());
        
        // 5. Validate new password is different
        passwordValidationService.validateNewPasswordDifferent(user, changePasswordRequestDto.getNewPassword());
        
        // 6. Update password
        String encodedNewPassword = passwordEncoder.encode(changePasswordRequestDto.getNewPassword());
        user.setUserPassword(encodedNewPassword);
        userRepository.save(user);
        
        logger.info("Password changed successfully for user: {}", userEmail);
        return true;
    }
}
