package com.example.demo.adapter.persistence.article;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "articles") 
public class ArticleEntity {
  @Id
  private UUID id;

  private UUID authorId;
  private String title;

  @Column(columnDefinition = "text")
  private String content;

  private Boolean isPublic;

  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;

  public ArticleEntity() {}

  @PrePersist void prePersist() {
    if (id == null) id = UUID.randomUUID();
    var now = OffsetDateTime.now();
    createdAt = now; 
    updatedAt = now;
  }

  @PreUpdate void preUpdate() { 
    updatedAt = OffsetDateTime.now(); 
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  } 
  
  public void setTitle(String title) {
    this.title=title;
  }
  public String getContent() {
    return content;
  }
  public void setContent(String content) {
    this.content=content;
  }
  public UUID getAuthorId() {
    return authorId;
  }
  public void setAuthorId(UUID authorId) {
    this.authorId=authorId;
  }
  public boolean isPublic() {
    return isPublic;
  }
  public void setPublic(boolean isPublic) {
    this.isPublic=isPublic;
  }
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt=createdAt;
  }
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt=updatedAt;
  }
}