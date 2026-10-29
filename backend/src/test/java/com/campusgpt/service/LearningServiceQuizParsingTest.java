package com.campusgpt.service;

import com.campusgpt.entity.DocumentEntity;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LearningServiceQuizParsingTest {
  @Test
  void parsesStructuredJsonQuiz() {
    DocumentEntity doc = new DocumentEntity();
    doc.setId(7L);
    doc.setFilename("dbms.pdf");
    String raw = """
        {
          "questions": [
            {
              "question": "Which normal form removes transitive dependency?",
              "options": ["1NF", "2NF", "3NF", "BCNF"],
              "correctOption": 3,
              "explanation": "3NF removes transitive dependencies."
            }
          ]
        }
        """;
    var questions = LearningService.parseQuiz(raw, doc, 5);
    assertEquals(1, questions.size());
    assertEquals("Which normal form removes transitive dependency?", questions.getFirst().question());
    assertEquals(List.of("1NF", "2NF", "3NF", "BCNF"), questions.getFirst().options());
    assertEquals(2, questions.getFirst().correctOption());
  }

  @Test
  void fallsBackToTextParsingWhenJsonIsNotReturned() {
    DocumentEntity doc = new DocumentEntity();
    doc.setId(9L);
    doc.setFilename("os.pdf");
    String raw = """
        Q1. Which scheduling algorithm can cause starvation?
        A) FCFS
        B) Round Robin
        C) Priority Scheduling
        D) SJF
        Answer: C
        Explanation: Low-priority processes can wait indefinitely.
        """;
    var questions = LearningService.parseQuiz(raw, doc, 5);
    assertEquals(1, questions.size());
    assertEquals("Which scheduling algorithm can cause starvation?", questions.getFirst().question());
    assertEquals(2, questions.getFirst().correctOption());
    assertEquals("Low-priority processes can wait indefinitely.", questions.getFirst().explanation());
  }
}
