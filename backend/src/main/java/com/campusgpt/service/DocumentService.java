package com.campusgpt.service;

import com.campusgpt.dto.Dto.DocumentResponse;
import com.campusgpt.entity.*;
import com.campusgpt.exception.ApiException;
import com.campusgpt.rag.RagProperties;
import com.campusgpt.repository.DocumentRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.*;

@Service
public class DocumentService {
  private final DocumentRepository documents;
  private final PdfExtractionService pdfs;
  private final TextChunker chunker;
  private final VectorStore vectorStore;
  private final RagProperties rag;
  private final Path uploadDir;

  public DocumentService(DocumentRepository documents, PdfExtractionService pdfs, TextChunker chunker, VectorStore vectorStore,
                         RagProperties rag, @Value("${campusgpt.upload-dir}") String uploadDir) {
    this.documents = documents; this.pdfs = pdfs; this.chunker = chunker; this.vectorStore = vectorStore; this.rag = rag;
    this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
  }

  @Transactional
  public DocumentResponse upload(MultipartFile file) {
    validatePdf(file);
    try {
      Files.createDirectories(uploadDir);
      String stored = UUID.randomUUID() + ".pdf";
      Path target = uploadDir.resolve(stored).normalize();
      if (!target.startsWith(uploadDir)) throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid storage path.");
      file.transferTo(target);
      DocumentEntity entity = new DocumentEntity();
      entity.setFilename(Optional.ofNullable(file.getOriginalFilename()).orElse("document.pdf"));
      entity.setStoredFilename(stored);
      entity.setContentType(Optional.ofNullable(file.getContentType()).orElse("application/pdf"));
      entity.setFileSize(file.getSize());
      entity.setStoragePath(target.toString());
      entity.setStatus(DocumentStatus.PENDING);
      entity = documents.save(entity);
      index(entity.getId());
      return map(entity);
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload failed: " + e.getMessage());
    }
  }

  @Transactional
  public void index(Long id) {
    DocumentEntity entity = get(id);
    entity.setStatus(DocumentStatus.PROCESSING);
    entity.setErrorMessage(null);
    try {
      List<Document> chunks = new ArrayList<>();
      for (PdfExtractionService.PageText page : pdfs.extract(Path.of(entity.getStoragePath()).toFile())) {
        int n = 0;
        for (String text : chunker.chunk(page.text(), rag.chunkSize(), rag.chunkOverlap())) {
          Map<String, Object> meta = new HashMap<>();
          meta.put("documentId", entity.getId());
          meta.put("filename", entity.getFilename());
          meta.put("page", page.pageNumber());
          meta.put("chunkId", entity.getId() + "-p" + page.pageNumber() + "-c" + (++n));
          chunks.add(new Document(text, meta));
        }
      }
      vectorStore.add(chunks);
      entity.setStatus(DocumentStatus.INDEXED);
    } catch (Exception e) {
      entity.setStatus(DocumentStatus.FAILED);
      entity.setErrorMessage(e.getMessage());
    }
  }

  @Transactional
  public void rebuildIndexedDocuments() {
    documents.findAll().stream()
        .filter(document -> document.getStatus() == DocumentStatus.INDEXED)
        .forEach(document -> index(document.getId()));
  }

  public List<DocumentResponse> list() { return documents.findAll().stream().map(this::map).toList(); }
  public DocumentEntity get(Long id) { return documents.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document not found.")); }
  @Transactional public void delete(Long id) {
    DocumentEntity doc = get(id);
    try { Files.deleteIfExists(Path.of(doc.getStoragePath())); } catch (Exception ignored) {}
    documents.delete(doc);
  }
  private void validatePdf(MultipartFile file) {
    if (file == null || file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Upload a non-empty PDF.");
    String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase();
    if (!name.endsWith(".pdf")) throw new ApiException(HttpStatus.BAD_REQUEST, "Only PDF files are supported.");
  }
  public DocumentResponse map(DocumentEntity d) {
    return new DocumentResponse(d.getId(), d.getFilename(), d.getFileSize(), d.getContentType(), d.getStatus(), d.getErrorMessage(), d.getCreatedAt(), d.getUpdatedAt());
  }
}
