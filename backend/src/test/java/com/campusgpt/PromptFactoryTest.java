package com.campusgpt;

import com.campusgpt.rag.PromptFactory;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PromptFactoryTest {
  @Test void promptContainsSafetyAndMetadata() {
    var p = new PromptFactory().groundedQuestion("What is JVM?", java.util.List.of(new Document("JVM runs bytecode", Map.of("filename", "java.pdf", "page", 3))));
    assertTrue(p.contains("Do not invent"));
    assertTrue(p.contains("java.pdf"));
    assertTrue(p.contains("JVM runs bytecode"));
  }
}
