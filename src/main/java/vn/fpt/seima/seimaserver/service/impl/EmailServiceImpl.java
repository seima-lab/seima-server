package vn.fpt.seima.seimaserver.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import vn.fpt.seima.seimaserver.service.EmailService;

import java.io.File;
import java.nio.charset.StandardCharsets;

@Service
public class EmailServiceImpl implements EmailService {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.sender.name:Seima Team}")
    private String senderName;

    @Value("${app.email.sender.email:noreply@seima.app}")
    private String senderEmail;

    // 1.Send mail simple Text
    @Override
    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderName + " <" + fromEmail + ">");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            javaMailSender.send(message);
            logger.info("Email sent successfully to " + to);
        } catch (Exception e) {
            logger.warn("Error sending email: " + e.getMessage());
        }
    }

    //2.Send mail with html template
    @Override
    public void sendHtmlMessage(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name()); // true = multipart message

            helper.setFrom(senderName + " <" + fromEmail + ">");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = isHtml

            javaMailSender.send(message);
            System.out.println("HTML Email sent successfully to " + to);
        } catch (MessagingException e) {
            System.err.println("Error sending HTML email: " + e.getMessage());
        }
    }

    // Send mail with attachments
    @Override
    public void sendMessageWithAttachment(String to, String subject, String text, File attachmentFile) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true); // true = multipart message

            helper.setFrom(senderName + " <" + fromEmail + ">");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            helper.addAttachment(attachmentFile.getName(), attachmentFile);

            javaMailSender.send(message);
            System.out.println("Email with attachment sent successfully to " + to);
        } catch (MessagingException e) {
            System.err.println("Error sending email with attachment: " + e.getMessage());
        }
    }

    // Send with html Template having data in template
    @Override
    @Async("taskExecutor")
    public void sendEmailWithHtmlTemplate(String to, String subject, String templateName, Context context) {
        if (templateEngine == null) {
            logger.error("TemplateEngine not configured. Cannot send email with template.");
            sendSimpleMessage(to, subject, "Please configure TemplateEngine to view this email properly. Context: " + context.getVariableNames());
            return;
        }
        
        // Development mode: log email instead of sending if credentials are not configured
        if (fromEmail == null || fromEmail.contains("${") || fromEmail.isEmpty()) {
            logger.warn("Email credentials not configured. Logging email instead of sending:");
            logger.info("=== EMAIL WOULD BE SENT ===");
            logger.info("To: {}", to);
            logger.info("Subject: {}", subject);
            logger.info("Template: {}", templateName);
            logger.info("Context variables: {}", context.getVariableNames());
            
            try {
                String htmlContent = templateEngine.process(templateName, context);
                logger.info("HTML Content: {}", htmlContent);
            } catch (Exception e) {
                logger.error("Error processing template: {}", e.getMessage());
            }
            
            logger.info("=== END EMAIL LOG ===");
            return;
        }
        
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            String htmlContent = templateEngine.process(templateName, context);

            helper.setTo(to);
            helper.setFrom(senderName + " <" + fromEmail + ">");
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = isHtml
            javaMailSender.send(mimeMessage);
            logger.info("Template Email sent successfully to {}", to);
        } catch (MessagingException e) {
            logger.error("Error sending template email: {}", e.getMessage());
            // Log the email for debugging
            logger.info("Failed email - To: {}, Subject: {}, Template: {}", to, subject, templateName);
        }
    }
}
