package com.campusgpt.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;

@Configuration
@Profile("local")
public class LocalAiConfig {
  private static final int EMBEDDING_DIMENSIONS = 768;

  @Bean
  EmbeddingModel localEmbeddingModel(
      @Value("${campusgpt.ollama.base-url}") String baseUrl,
      @Value("${campusgpt.ollama.embedding-model}") String model) {
    RestClient client = RestClient.builder().baseUrl(baseUrl).build();
    return new EmbeddingModel() {
      @Override
      public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        int index = 0;
        for (String text : request.getInstructions()) {
          embeddings.add(new Embedding(ollamaEmbedding(client, model, text), index++));
        }
        return new EmbeddingResponse(embeddings);
      }

      @Override
      public float[] embed(Document document) {
        return ollamaEmbedding(client, model, document.getText());
      }

      @Override
      public int dimensions() {
        return EMBEDDING_DIMENSIONS;
      }
    };
  }

  @Bean
  VectorStore localVectorStore(EmbeddingModel embeddingModel) {
    return SimpleVectorStore.builder(embeddingModel).build();
  }

  @Bean
  ChatModel localChatModel(
      @Value("${campusgpt.ollama.base-url}") String baseUrl,
      @Value("${campusgpt.ollama.chat-model}") String model) {
    RestClient client = RestClient.builder().baseUrl(baseUrl).build();
    return new ChatModel() {
      @Override
      public ChatResponse call(Prompt prompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("prompt", prompt.getContents());
        body.put("stream", false);
        body.put("options", Map.of("temperature", 0.1));
        JsonNode response = client.post().uri("/api/generate").body(body).retrieve().body(JsonNode.class);
        if (response == null || !response.hasNonNull("response")) {
          throw new IllegalStateException("Ollama returned an empty response.");
        }
        return new ChatResponse(List.of(new Generation(new AssistantMessage(response.get("response").asText().trim()))));
      }
    };
  }

  private static float[] ollamaEmbedding(RestClient client, String model, String text) {
    Map<String, Object> body = Map.of("model", model, "input", List.of(text));
    JsonNode response = client.post().uri("/api/embed").body(body).retrieve().body(JsonNode.class);
    JsonNode values = response == null ? null : response.path("embeddings").path(0);
    if (values == null || !values.isArray() || values.size() != EMBEDDING_DIMENSIONS) {
      throw new IllegalStateException("Ollama returned an invalid embedding. Check the embedding model.");
    }
    float[] embedding = new float[values.size()];
    for (int i = 0; i < values.size(); i++) embedding[i] = (float) values.get(i).asDouble();
    return embedding;
  }
}
