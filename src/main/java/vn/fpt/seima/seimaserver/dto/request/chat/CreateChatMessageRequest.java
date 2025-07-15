package vn.fpt.seima.seimaserver.dto.request.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.fpt.seima.seimaserver.entity.SenderType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateChatMessageRequest {
    
    @Size(max = 255, message = "Conversation ID cannot exceed 255 characters")
    private String conversationId;
    
    @NotNull(message = "Sender type is required")
    private SenderType senderType;
    
    @NotBlank(message = "Message content is required")
    @Size(max = 10000, message = "Message content cannot exceed 10000 characters")
    private String messageContent;
} 