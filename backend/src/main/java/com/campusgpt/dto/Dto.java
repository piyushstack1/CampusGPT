package com.campusgpt.dto;

import com.campusgpt.entity.DocumentStatus;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public class Dto {
  public record DocumentResponse(Long id, String filename, long fileSize, String contentType, DocumentStatus status,
                                 String errorMessage, Instant createdAt, Instant updatedAt) {}
  public record DashboardResponse(long totalDocuments, long indexedDocuments, long totalConversations,
                                  List<DocumentResponse> recentDocuments, List<ChatSessionResponse> recentConversations) {}
  public record ChatSessionResponse(Long id, String title, Instant createdAt, Instant updatedAt) {}
  public record ChatMessageResponse(Long id, String role, String content, Instant createdAt) {}
  public record AskRequest(@NotBlank String question, Long sessionId, List<Long> documentIds) {}
  public record SourceResponse(Long documentId, String filename, Integer pageNumber, String chunkId, String excerpt) {}
  public record AskResponse(String answer, List<SourceResponse> sources, boolean grounded, Long sessionId) {}
  public record SummaryResponse(Long documentId, String summary, List<SourceResponse> sources) {}
  public record QuizGenerateRequest(@NotNull Long documentId, @Min(1) @Max(20) Integer questionCount) {}
  public record QuizQuestionResponse(String question, List<String> options, int correctOption, String explanation, SourceResponse source) {}
  public record QuizResponse(Long id, Long documentId, List<QuizQuestionResponse> questions) {}
  public record ErrorResponse(String code, String message, Instant timestamp) {}
}
