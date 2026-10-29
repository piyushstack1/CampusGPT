package com.campusgpt.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "documents")
public class DocumentEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String filename;
  private String storedFilename;
  private String contentType;
  private long fileSize;
  private String storagePath;
  @Enumerated(EnumType.STRING)
  private DocumentStatus status;
  @Column(columnDefinition = "text")
  private String errorMessage;
  private Instant createdAt = Instant.now();
  private Instant updatedAt = Instant.now();

  @PreUpdate public void touch() { updatedAt = Instant.now(); }
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getFilename() { return filename; }
  public void setFilename(String filename) { this.filename = filename; }
  public String getStoredFilename() { return storedFilename; }
  public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }
  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
  public long getFileSize() { return fileSize; }
  public void setFileSize(long fileSize) { this.fileSize = fileSize; }
  public String getStoragePath() { return storagePath; }
  public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
  public DocumentStatus getStatus() { return status; }
  public void setStatus(DocumentStatus status) { this.status = status; }
  public String getErrorMessage() { return errorMessage; }
  public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
