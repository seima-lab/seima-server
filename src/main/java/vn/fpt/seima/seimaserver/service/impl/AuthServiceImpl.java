package vn.fpt.seima.seimaserver.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import vn.fpt.seima.seimaserver.dto.request.auth.NormalRegisterRequestDto;
import vn.fpt.seima.seimaserver.exception.GmailAlreadyExistException;
import vn.fpt.seima.seimaserver.exception.NullRequestParamException;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.AuthService;
import vn.fpt.seima.seimaserver.service.EmailService;
import vn.fpt.seima.seimaserver.util.OtpUtils;

import java.util.HashMap;

@Service
public class AuthServiceImpl implements AuthService {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;

    @Value("${app.lab-name}")
    private String labName;

    @Value("${app.email.otp-register.html-template}")
    private String otpRegisterHtmlTemplate;

    @Override
    public void logout(HttpServletRequest request) {

    }

    @Override
    public String processRegister(NormalRegisterRequestDto normalRegisterRequestDto) {

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
        return otp;
    }
}
