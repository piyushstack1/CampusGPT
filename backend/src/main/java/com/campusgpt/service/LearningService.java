package com.campusgpt.service;

import com.campusgpt.dto.Dto.*;
import com.campusgpt.entity.DocumentEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LearningService {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Pattern OPTION_PATTERN = Pattern.compile("^\\s*(?:[A-Da-d]|[1-4])[\\).:\\-]\\s+(.+)$");
  private static final Pattern ANSWER_PATTERN = Pattern.compile("^\\s*(?:answer|correct(?:\\s+answer)?)\\s*[:\\-]?\\s*([A-Da-d1-4]).*$", Pattern.CASE_INSENSITIVE);
  private static final Pattern EXPLANATION_PATTERN = Pattern.compile("^\\s*explanation\\s*[:\\-]?\\s*(.+)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern QUESTION_PREFIX_PATTERN = Pattern.compile("^\\s*(?:q\\s*\\d+|question\\s*\\d+|\\d+)[\\).:\\-]?\\s*", Pattern.CASE_INSENSITIVE);

  private final DocumentService documents;
  private final PdfExtractionService pdfs;
  private final ChatClient chatClient;
  public LearningService(DocumentService documents, PdfExtractionService pdfs, ChatClient.Builder builder) {
    this.documents = documents;
    this.pdfs = pdfs;
    this.chatClient = builder.build();
  }
  public SummaryResponse summary(Long id) {
    DocumentEntity doc = documents.get(id);
    String text = joinedText(doc, 12000);
    String summary = chatClient.prompt("Summarize this academic document with headings, key concepts, and exam points. Text:\n" + text).call().content();
    return new SummaryResponse(id, summary, List.of(new SourceResponse(id, doc.getFilename(), null, "summary-scope", "Summary generated from extracted document text.")));
  }
  public QuizResponse quiz(QuizGenerateRequest request) {
    DocumentEntity doc = documents.get(request.documentId());
    int requestedCount = request.questionCount();
    String prompt = """
        You are creating exam-ready multiple choice questions from study material.
        Create exactly %d MCQs from the text below.
        Return ONLY JSON with this shape:
        {
          "questions": [
            {
              "question": "string",
              "options": ["string", "string", "string", "string"],
              "correctOption": 1,
              "explanation": "string"
            }
          ]
        }
        Rules:
        - Keep questions unambiguous and grounded in the text.
        - options must be exactly 4 items.
        - correctOption must be 1-4.
        - No markdown fences, no extra keys, no extra commentary.
        Text:
        %s
        """.formatted(requestedCount, joinedText(doc, 9000));
    String raw = chatClient.prompt(prompt).call().content();
    List<QuizQuestionResponse> questions = parseQuiz(raw, doc, requestedCount);
    if (questions.isEmpty()) {
      throw new IllegalStateException("Quiz generation failed. Try again with fewer questions or reindex the document.");
    }
    return new QuizResponse(null, doc.getId(), questions);
  }

  static List<QuizQuestionResponse> parseQuiz(String raw, DocumentEntity doc, int requestedCount) {
    SourceResponse source = new SourceResponse(
        doc.getId(),
        doc.getFilename(),
        null,
        "quiz-scope",
        "Quiz generated from extracted document text.");
    List<QuizQuestionResponse> parsed = parseJsonQuiz(raw, source, requestedCount);
    if (!parsed.isEmpty()) {
      return parsed;
    }
    return parseTextQuiz(raw, source, requestedCount);
  }

  static List<QuizQuestionResponse> parseJsonQuiz(String raw, SourceResponse source, int requestedCount) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    String cleaned = raw.trim();
    if (cleaned.startsWith("```")) {
      cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
      cleaned = cleaned.replaceFirst("\\s*```\\s*$", "");
    }
    try {
      JsonNode root = OBJECT_MAPPER.readTree(cleaned);
      JsonNode questionsNode = root.path("questions");
      if (!questionsNode.isArray()) {
        return List.of();
      }
      List<QuizQuestionResponse> questions = new ArrayList<>();
      for (JsonNode node : questionsNode) {
        String question = node.path("question").asText("").trim();
        JsonNode optionsNode = node.path("options");
        String explanation = node.path("explanation").asText("").trim();
        int oneBasedCorrect = node.path("correctOption").asInt(-1);
        if (question.isBlank() || !optionsNode.isArray() || optionsNode.size() != 4) {
          continue;
        }
        List<String> options = new ArrayList<>(4);
        for (JsonNode option : optionsNode) {
          String value = option.asText("").trim();
          if (value.isBlank()) {
            options.clear();
            break;
          }
          options.add(value);
        }
        if (options.size() != 4) {
          continue;
        }
        int zeroBased = oneBasedCorrect >= 1 && oneBasedCorrect <= 4 ? oneBasedCorrect - 1 : -1;
        if (zeroBased < 0) {
          continue;
        }
        questions.add(new QuizQuestionResponse(question, options, zeroBased, explanation.isBlank() ? "Review the source section and verify why this answer is correct." : explanation, source));
        if (questions.size() >= requestedCount) {
          break;
        }
      }
      return questions;
    } catch (Exception ignored) {
      return List.of();
    }
  }

  static List<QuizQuestionResponse> parseTextQuiz(String raw, SourceResponse source, int requestedCount) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    List<QuizQuestionResponse> result = new ArrayList<>();
    String question = null;
    List<String> options = new ArrayList<>(4);
    int correctOption = -1;
    String explanation = "";
    String[] lines = raw.replace("\r\n", "\n").split("\n");
    for (String line : lines) {
      String trimmed = line == null ? "" : line.trim();
      if (trimmed.isBlank()) {
        continue;
      }
      Matcher optionMatcher = OPTION_PATTERN.matcher(trimmed);
      if (optionMatcher.matches()) {
        if (options.size() < 4) {
          options.add(optionMatcher.group(1).trim());
        }
        continue;
      }
      Matcher answerMatcher = ANSWER_PATTERN.matcher(trimmed);
      if (answerMatcher.matches()) {
        correctOption = toZeroBased(answerMatcher.group(1));
        continue;
      }
      Matcher explanationMatcher = EXPLANATION_PATTERN.matcher(trimmed);
      if (explanationMatcher.matches()) {
        explanation = explanationMatcher.group(1).trim();
        continue;
      }
      String candidate = QUESTION_PREFIX_PATTERN.matcher(trimmed).replaceFirst("").trim();
      if (looksLikeQuestion(candidate)) {
        if (canFinalize(question, options, correctOption)) {
          result.add(new QuizQuestionResponse(question, List.copyOf(options), correctOption, explanation.isBlank() ? "Review the source section and verify why this answer is correct." : explanation, source));
          if (result.size() >= requestedCount) {
            return result;
          }
          options = new ArrayList<>(4);
          correctOption = -1;
          explanation = "";
        }
        question = candidate;
      }
    }
    if (canFinalize(question, options, correctOption) && result.size() < requestedCount) {
      result.add(new QuizQuestionResponse(question, List.copyOf(options), correctOption, explanation.isBlank() ? "Review the source section and verify why this answer is correct." : explanation, source));
    }
    return result;
  }

  private static boolean canFinalize(String question, List<String> options, int correctOption) {
    return question != null && !question.isBlank() && options.size() == 4 && correctOption >= 0 && correctOption < 4;
  }

  private static boolean looksLikeQuestion(String text) {
    if (text == null || text.isBlank()) {
      return false;
    }
    String normalized = text.toLowerCase(Locale.ROOT);
    return text.endsWith("?")
        || normalized.startsWith("which ")
        || normalized.startsWith("what ")
        || normalized.startsWith("when ")
        || normalized.startsWith("where ")
        || normalized.startsWith("why ")
        || normalized.startsWith("how ");
  }

  private static int toZeroBased(String token) {
    String value = token.trim();
    if (value.matches("[1-4]")) {
      return Integer.parseInt(value) - 1;
    }
    char c = Character.toUpperCase(value.charAt(0));
    if (c >= 'A' && c <= 'D') {
      return c - 'A';
    }
    return -1;
  }

  private String joinedText(DocumentEntity doc, int max) {
    String text = pdfs.extract(java.nio.file.Path.of(doc.getStoragePath()).toFile()).stream().map(PdfExtractionService.PageText::text).reduce("", (a, b) -> a + "\n" + b);
    return text.substring(0, Math.min(max, text.length()));
  }
}
