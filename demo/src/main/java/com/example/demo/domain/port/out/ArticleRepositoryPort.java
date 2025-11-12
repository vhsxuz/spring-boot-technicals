package com.example.demo.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.demo.domain.model.Article;

public interface ArticleRepositoryPort {
  Article save(Article article);
  Optional<Article> findById(UUID id);
  List<Article> find(UUID authorId, Boolean isPublic, int limit, int offset);
  long count(UUID authorId, Boolean isPublic);
  Optional<Article> update(Article article);
  boolean delete(UUID id);
}
