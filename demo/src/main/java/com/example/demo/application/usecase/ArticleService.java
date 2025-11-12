package com.example.demo.application.usecase;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.demo.domain.model.Article;
import com.example.demo.domain.port.in.ArticleUseCase;
import com.example.demo.domain.port.out.ArticleRepositoryPort;

@Service
public class ArticleService implements ArticleUseCase {

  private final ArticleRepositoryPort repo;

  public ArticleService(ArticleRepositoryPort repo) {
    this.repo = repo;
  }

  @Override
  public   Article create(Article article) {
    var now = OffsetDateTime.now();
    var newArticle = new Article(
      UUID.randomUUID(),
      Objects.requireNonNull(article.title(), "title is required"),
      Objects.requireNonNull(article.content(), "content is required"),
      Objects.requireNonNull(article.authorId(), "authorId is required"),
      article.isPublic(),
      now, 
      now
    );
    return repo.save(newArticle);
  }

  @Override
  public Optional<Article> getById(UUID id) {
    return repo.findById(id);
  }

  @Override
  public List<Article> list(UUID authorId, Boolean isPublic, int limit, int offset) {
    return repo.find(authorId, isPublic, limit, offset);
  }

  @Override
  public Optional<Article> update(Article article) {
    var existingOpt = repo.findById(article.id());
    if (existingOpt.isEmpty()) {
      return Optional.empty();
    }
    var existing = existingOpt.get();
    var updated = new Article(
      existing.id(),
      Objects.requireNonNull(article.title(), "title is required"),
      Objects.requireNonNull(article.content(), "content is required"),
      existing.authorId(),
      article.isPublic(),
      existing.createdAt(),
      OffsetDateTime.now()
    );
    return repo.update(updated);
  }

  @Override
  public boolean delete(UUID id) {
    return repo.delete(id);
  }

  @Override
  public long count(UUID authorId, Boolean isPublic) {
    return repo.count(authorId, isPublic);
  }
}
