package com.example.demo.domain.port.in;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.demo.domain.model.Article;

public interface ArticleUseCase {
  Article create(Article article);
  Optional<Article> getById(UUID id);
  List<Article> list(UUID authorId, Boolean isPublic, int limit, int offset);
  long count(UUID authorId, Boolean isPublic);
  Optional<Article> update(Article article);
  boolean delete(UUID id);
}
