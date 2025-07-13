package vn.fpt.seima.seimaserver.service.impl;

import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.service.FcmService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FcmServiceImpl implements FcmService {

    private static final Logger logger = LoggerFactory.getLogger(FcmServiceImpl.class);
    
    // Constants
    private static final String EMPTY_TOKEN_MESSAGE = "Token list is empty. No message sent.";
    private static final String SUCCESS_MESSAGE = "{} messages were sent successfully.";
    private static final String FAILED_TOKENS_MESSAGE = "Messages failed to send to {} tokens: {}";
    private static final String FCM_ERROR_MESSAGE = "Error sending multicast message to FCM.";
    
    @Override
    public BatchResponse sendMulticastNotification(List<String> tokens, String title, String body, Map<String, String> data) {
        // Input validation
        validateInputs(tokens, title, body);
        
        logger.info("Sending notification to {} tokens. Title: {}", tokens.size(), title);
        
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(notification)
                .putAllData(data != null ? data : Map.of())
                .addAllTokens(tokens)
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(message);
            logger.info(SUCCESS_MESSAGE, response.getSuccessCount());

            if (response.getFailureCount() > 0) {
                handleFailedTokens(response, tokens);
            }
            return response;
        } catch (FirebaseMessagingException e) {
            logger.error(FCM_ERROR_MESSAGE, e);
            throw new RuntimeException("Failed to send Firebase multicast message", e);
        }
    }

    private void validateInputs(List<String> tokens, String title, String body) {
        if (tokens == null || tokens.isEmpty()) {
            logger.warn(EMPTY_TOKEN_MESSAGE);
            throw new IllegalArgumentException("Token list cannot be null or empty");
        }
        
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("Body cannot be null or empty");
        }
    }

    private void handleFailedTokens(BatchResponse response, List<String> tokens) {
        List<SendResponse> responses = response.getResponses();
        List<String> failedTokens = new ArrayList<>();

        for (int i = 0; i < responses.size(); i++) {
            if (!responses.get(i).isSuccessful()) {
                failedTokens.add(tokens.get(i));
                // Log specific error for debugging
                logger.debug("Token {} failed with error: {}", 
                    tokens.get(i), 
                    responses.get(i).getException().getMessage());
            }
        }
        logger.warn(FAILED_TOKENS_MESSAGE, failedTokens.size(), failedTokens);
    }
}
