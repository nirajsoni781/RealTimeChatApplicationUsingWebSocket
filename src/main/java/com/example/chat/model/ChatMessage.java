package com.example.chat.model;

import lombok.*;		//No need for constructor / Getter / Setter / toString() > Lombok automatically create it at compiletime

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
  private String sender;    // "user1"
  private String receiver;  // "user2"
  private String content;   // message text
  private long timestamp;   // epoch millis
}
