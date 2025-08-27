package com.example.chat.controller;

import com.example.chat.model.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

  private final SimpMessagingTemplate messagingTemplate;

  public ChatController(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  // Client sends to /app/chat.send
  @MessageMapping("/chat.send")
  public void send(@Payload ChatMessage message) {
    // Deliver privately to the receiver's personal queue:
    // Client should subscribe to /user/queue/messages
    messagingTemplate.convertAndSendToUser(
        message.getReceiver(),
        "/queue/messages",
        message
    );
  }
}
