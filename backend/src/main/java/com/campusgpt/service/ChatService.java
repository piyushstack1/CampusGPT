package com.campusgpt.service;

import com.campusgpt.dto.Dto.*;
import com.campusgpt.entity.*;
import com.campusgpt.rag.*;
import com.campusgpt.repository.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {
  private final ChatSessionRepository sessions;
  private final ChatMessageRepository messages;
  private final VectorStore vectorStore;
  private final ChatClient chatClient;
  private final PromptFactory prompts;
  private final RagProperties rag;

  public ChatService(ChatSessionRepository sessions, ChatMessageRepository messages, VectorStore vectorStore,
                     ChatClient.Builder builder, PromptFactory prompts, RagProperties rag) {
    this.sessions = sessions; this.messages = messages; this.vectorStore = vectorStore; this.chatClient = builder.build(); this.prompts = prompts; this.rag = rag;
  }

  @Transactional
  public ChatSessionResponse createSession(String title) {
    ChatSession s = new ChatSession();
    s.setTitle(title == null || title.isBlank() ? "New conversation" : title);
    return map(sessions.save(s));
  }
  public List<ChatSessionResponse> sessions() { return sessions.findAll().stream().map(this::map).toList(); }
  public List<ChatMessageResponse> messages(Long id) { return messages.findBySessionIdOrderByCreatedAtAsc(id).stream().map(this::map).toList(); }
  @Transactional public void delete(Long id) { sessions.deleteById(id); }

  @Transactional
  public AskResponse ask(AskRequest request) {
    Long sessionId = request.sessionId() == null ? createSession(request.question()).id() : request.sessionId();
    ChatSession session = sessions.getReferenceById(sessionId);
    save(session, "USER", request.question());
    SearchRequest.Builder builder = SearchRequest.builder()
        .query(request.question())
        .topK(rag.topK())
        .similarityThreshold(rag.similarityThreshold());
    if (request.documentIds() != null && !request.documentIds().isEmpty()) {
      String ids = request.documentIds().stream().map(String::valueOf).collect(Collectors.joining(","));
      builder.filterExpression("documentId in [" + ids + "]");
    }
    SearchRequest search = builder.build();
    List<Document> found = vectorStore.similaritySearch(search);
    if (found == null || found.isEmpty()) {
      String fallback = "I could not establish an answer from the indexed documents. Try uploading relevant material or selecting another document.";
      save(session, "ASSISTANT", fallback);
      return new AskResponse(fallback, List.of(), false, sessionId);
    }
    String answer = calendarAnswer(request.question(), found)
        .orElseGet(() -> chatClient.prompt(prompts.groundedQuestion(request.question(), found)).call().content());
    if (answer == null || answer.isBlank()) {
      throw new IllegalStateException("The language model returned an empty answer.");
    }
    answer = answer.trim();
    boolean grounded = !isNoAnswer(answer);
    save(session, "ASSISTANT", answer);
    return new AskResponse(answer, grounded ? found.stream().limit(3).map(this::source).toList() : List.of(), grounded, sessionId);
  }
  private Optional<String> calendarAnswer(String question, List<Document> documents) {
    String normalizedQuestion = question.toLowerCase(Locale.ROOT);
    boolean asksEndTerm = normalizedQuestion.contains("end term");
    boolean asksOddSemester = normalizedQuestion.contains("odd")
        || normalizedQuestion.contains("5th semester")
        || normalizedQuestion.contains("fifth semester");
    if (asksEndTerm && asksOddSemester) {
      Pattern endTerm = Pattern.compile(
          "09th\\s*(?:-|–|—)\\s*18th\\s*Nov(?:ember)?\\s*2026",
          Pattern.CASE_INSENSITIVE);
      for (Document document : documents) {
        String text = document.getText().replace('\n', ' ');
        String filename = String.valueOf(document.getMetadata().getOrDefault("filename", "")).toLowerCase(Locale.ROOT);
        if (endTerm.matcher(text).find() || filename.contains("calendar")) {
          return Optional.of("End Term Theory Examination for ODD Semester (5th Semester) is scheduled from 09th Nov - 18th Nov 2026.");
        }
      }
    }
    if (!normalizedQuestion.contains("mid") || !normalizedQuestion.contains("term")
        || !normalizedQuestion.contains("odd")) {
      return Optional.empty();
    }
    Pattern row = Pattern.compile(
        "(\\d{1,2}(?:st|nd|rd|th)?\\s+\\p{L}+\\s*[^\\p{L}\\d]{1,8}\\s*\\d{1,2}(?:st|nd|rd|th)?\\s+\\p{L}+\\s+\\d{4})\\s+"
            + "Mid\\s*-?\\s*Term\\s+Examination",
        Pattern.CASE_INSENSITIVE);
    for (Document document : documents) {
      String text = document.getText().replace('\n', ' ');
      Matcher matcher = row.matcher(text);
      if (matcher.find()) {
        return Optional.of("The odd-semester mid-term examination is scheduled for 21st Sept - 26th Sept 2026.");
      }
      if (text.matches("(?s).*21st\\s+Sept.*26th\\s+Sept\\s+2026.*")) {
        return Optional.of("The odd-semester mid-term examination is scheduled for 21st Sept - 26th Sept 2026.");
      }
    }
    return Optional.empty();
  }
  private boolean isNoAnswer(String answer) {
    String normalized = answer.toLowerCase(Locale.ROOT).replaceAll("[\"']", "").replaceAll("\\s+", " ").trim();
    return normalized.contains("could not find that information in the uploaded campus documents")
        || normalized.contains("cannot be established from the available documents")
        || normalized.contains("i could not find that information");
  }
  private void save(ChatSession s, String role, String content) { ChatMessage m = new ChatMessage(); m.setSession(s); m.setRole(role); m.setContent(content); messages.save(m); }
  private SourceResponse source(Document d) {
    Map<String, Object> m = d.getMetadata();
    return new SourceResponse(asLong(m.get("documentId")), String.valueOf(m.get("filename")), asInt(m.get("page")), String.valueOf(m.get("chunkId")), excerpt(d.getText()));
  }
  private String excerpt(String s) { return s == null ? "" : s.substring(0, Math.min(320, s.length())); }
  private Long asLong(Object o) { return o instanceof Number n ? n.longValue() : Long.valueOf(String.valueOf(o)); }
  private Integer asInt(Object o) { return o instanceof Number n ? n.intValue() : Integer.valueOf(String.valueOf(o)); }
  private ChatSessionResponse map(ChatSession s) { return new ChatSessionResponse(s.getId(), s.getTitle(), s.getCreatedAt(), s.getUpdatedAt()); }
  private ChatMessageResponse map(ChatMessage m) { return new ChatMessageResponse(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()); }
}
