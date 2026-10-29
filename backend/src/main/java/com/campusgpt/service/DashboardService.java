package com.campusgpt.service;

import com.campusgpt.dto.Dto.*;
import com.campusgpt.entity.DocumentStatus;
import com.campusgpt.repository.*;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
  private final DocumentRepository documents;
  private final ChatSessionRepository sessions;
  private final DocumentService documentService;
  private final ChatService chatService;
  public DashboardService(DocumentRepository documents, ChatSessionRepository sessions, DocumentService documentService, ChatService chatService) {
    this.documents = documents; this.sessions = sessions; this.documentService = documentService; this.chatService = chatService;
  }
  public DashboardResponse dashboard() {
    var docs = documents.findAll();
    return new DashboardResponse(docs.size(), docs.stream().filter(d -> d.getStatus() == DocumentStatus.INDEXED).count(), sessions.count(),
        docs.stream().limit(5).map(documentService::map).toList(), chatService.sessions().stream().limit(5).toList());
  }
}
