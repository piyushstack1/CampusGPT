package com.campusgpt.config;

import com.campusgpt.rag.RagProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class AppConfig implements WebMvcConfigurer {
  @Value("${campusgpt.frontend-origin}") private String origin;
  @Override public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**").allowedOrigins(origin).allowedMethods("GET", "POST", "DELETE", "OPTIONS").allowedHeaders("*");
  }
}
