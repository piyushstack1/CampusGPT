package com.campusgpt.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "session_id")
  private ChatSession session;
  private String role;
  @Column(columnDefinition = "text")
  private String content;
  private Instant createdAt = Instant.now();
  public Long getId() { return id; }
  public ChatSession getSession() { return session; }
  public void setSession(ChatSession session) { this.session = session; }
  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public Instant getCreatedAt() { return createdAt; }
}
