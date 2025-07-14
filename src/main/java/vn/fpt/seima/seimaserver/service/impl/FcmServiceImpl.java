package vn.fpt.seima.seimaserver.service.impl;

import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.service.FcmService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service implementation for sending Firebase Cloud Messaging (FCM) notifications.
 */
@Service
public class FcmServiceImpl implements FcmService {

    private static final Logger logger = LoggerFactory.getLogger(FcmServiceImpl.class);

    @Override
    public BatchResponse sendMulticastNotification(List<String> tokens, String title, String body, Map<String, String> data) {
        validateInputs(tokens, title, body);

        logger.info("Attempting to send notification to {} tokens. Title: '{}'", tokens.size(), title);

        // Build the multicast message
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
            // Switched to sendEachForMulticast for detailed responses without a dry-run parameter.
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            logger.info("Sent messages to {} tokens. Success count: {}, Failure count: {}",
                    tokens.size(), response.getSuccessCount(), response.getFailureCount());

            // Handle failed tokens with detailed logging.
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                List<String> failedTokens = new ArrayList<>();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        String token = tokens.get(i);
                        failedTokens.add(token);
                        // Log the specific reason for failure for each token
                        logger.warn("Token failed: {}. Reason: {}", token, responses.get(i).getException().getMessage());
                    }
                }
                logger.error("Failed to send messages to {} tokens: {}", failedTokens.size(), failedTokens);
            }

            return response;

        } catch (FirebaseMessagingException e) {
            // Catch fatal errors (e.g., authentication issues, connection problems)
            logger.error("A fatal error occurred while sending multicast message to FCM.", e);
            throw new RuntimeException("Failed to send Firebase multicast message", e);
        }
    }

    /**
     * Validates that essential inputs for sending a notification are not null or empty.
     */
    private void validateInputs(List<String> tokens, String title, String body) {
        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("Token list cannot be null or empty.");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty.");
        }
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("Body cannot be null or empty.");
        }
    }
}