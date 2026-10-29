package com.campusgpt.controller;

import com.campusgpt.dto.Dto.*;
import com.campusgpt.service.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final DocumentService documents;
  private final ChatService chat;
  private final DashboardService dashboard;
  private final LearningService learning;
  public ApiController(DocumentService documents, ChatService chat, DashboardService dashboard, LearningService learning) {
    this.documents = documents; this.chat = chat; this.dashboard = dashboard; this.learning = learning;
  }
  @GetMapping("/health") Map<String, String> health() { return Map.of("status", "UP", "service", "CampusGPT"); }
  @GetMapping("/dashboard") DashboardResponse dashboard() { return dashboard.dashboard(); }
  @PostMapping("/documents/upload") DocumentResponse upload(@RequestParam("file") MultipartFile file) { return documents.upload(file); }
  @GetMapping("/documents") List<DocumentResponse> listDocuments() { return documents.list(); }
  @GetMapping("/documents/{id}") DocumentResponse getDocument(@PathVariable Long id) { return documents.map(documents.get(id)); }
  @DeleteMapping("/documents/{id}") void deleteDocument(@PathVariable Long id) { documents.delete(id); }
  @PostMapping("/documents/{id}/reindex") DocumentResponse reindex(@PathVariable Long id) { documents.index(id); return documents.map(documents.get(id)); }
  @PostMapping("/documents/{id}/summary") SummaryResponse summary(@PathVariable Long id) { return learning.summary(id); }
  @PostMapping("/chat/sessions") ChatSessionResponse session(@RequestBody(required = false) Map<String, String> body) { return chat.createSession(body == null ? null : body.get("title")); }
  @GetMapping("/chat/sessions") List<ChatSessionResponse> sessions() { return chat.sessions(); }
  @GetMapping("/chat/sessions/{id}/messages") List<ChatMessageResponse> messages(@PathVariable Long id) { return chat.messages(id); }
  @DeleteMapping("/chat/sessions/{id}") void deleteSession(@PathVariable Long id) { chat.delete(id); }
  @PostMapping("/chat/ask") AskResponse ask(@Valid @RequestBody AskRequest request) { return chat.ask(request); }
  @PostMapping("/quiz/generate") QuizResponse quiz(@Valid @RequestBody QuizGenerateRequest request) { return learning.quiz(request); }
}
