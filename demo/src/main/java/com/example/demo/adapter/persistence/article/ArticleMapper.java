package com.example.demo.adapter.persistence.article;

import com.example.demo.domain.model.Article;

public class ArticleMapper {
  
  private ArticleMapper() {}

  static Article toDomain(ArticleEntity e) {
    return new Article(
      e.getId(), 
      e.getTitle(), 
      e.getContent(),
      e.getAuthorId(), 
      e.isPublic(), 
      e.getCreatedAt(), 
      e.getUpdatedAt()
    );
  }

  static ArticleEntity toEntity(Article a) {
    ArticleEntity e = new ArticleEntity();
    e.setId(a.id());
    e.setTitle(a.title());
    e.setContent(a.content());
    e.setAuthorId(a.authorId());
    e.setPublic(a.isPublic());
    e.setCreatedAt(a.createdAt());
    e.setUpdatedAt(a.updatedAt());
    return e;
  }
}
