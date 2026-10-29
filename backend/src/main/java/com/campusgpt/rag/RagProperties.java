package com.campusgpt.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campusgpt.rag")
public record RagProperties(int chunkSize, int chunkOverlap, int topK, double similarityThreshold) {}
