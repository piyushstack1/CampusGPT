package com.campusgpt.config;

import com.campusgpt.service.DocumentService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalIndexWarmup implements ApplicationRunner {
  private final DocumentService documents;

  public LocalIndexWarmup(DocumentService documents) {
    this.documents = documents;
  }

  @Override
  public void run(ApplicationArguments args) {
    documents.rebuildIndexedDocuments();
  }
}
