package com.campusgpt.service;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {
  public List<String> chunk(String text, int size, int overlap) {
    List<String> chunks = new ArrayList<>();
    String normalized = text
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .replaceAll("[\\t ]+", " ")
        .replaceAll("\\n{3,}", "\n\n")
        .trim();
    if (normalized.isBlank()) return chunks;
    int step = Math.max(1, size - overlap);
    for (int start = 0; start < normalized.length(); start += step) {
      int end = Math.min(normalized.length(), start + size);
      chunks.add(normalized.substring(start, end));
      if (end == normalized.length()) break;
    }
    return chunks;
  }
}
