package vn.fpt.seima.seimaserver.service;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.thymeleaf.context.Context;

import java.io.File;

public interface EmailService {
    void sendSimpleMessage(String to, String subject, String text);
    void sendHtmlMessage(String to, String subject, String htmlBody);
    void sendMessageWithAttachment(String to, String subject, String text, File attachmentFile);
    void sendEmailWithHtmlTemplate(String to, String subject, String templateName, Context context);
}
