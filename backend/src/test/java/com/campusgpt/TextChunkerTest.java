package com.campusgpt;

import com.campusgpt.service.TextChunker;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextChunkerTest {
  @Test void chunksWithOverlap() {
    var chunks = new TextChunker().chunk("abcdefghijklmnopqrstuvwxyz", 10, 2);
    assertEquals("abcdefghij", chunks.get(0));
    assertEquals("ijklmnopqr", chunks.get(1));
    assertFalse(chunks.isEmpty());
  }
}
