package com.example.demo.adapter.persistence.article;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import static com.example.demo.adapter.persistence.article.ArticleMapper.toDomain;
import static com.example.demo.adapter.persistence.article.ArticleMapper.toEntity;
import com.example.demo.domain.model.Article;
import com.example.demo.domain.port.out.ArticleRepositoryPort;
import com.example.demo.domain.utils.SortUtils;

@Component
public class ArticleRepositoryAdapter implements ArticleRepositoryPort {
  private final ArticleJpaRepository jpa;

  public ArticleRepositoryAdapter(ArticleJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Article save(Article article) {
    var entity = toEntity(article);
    var savedEntity = jpa.save(entity);
    return toDomain(savedEntity);
  }

  @Override
  public Optional<Article> findById(UUID id) {
    return jpa.findById(id).map(ArticleMapper::toDomain);
  }

  @Override
  public List<Article> find(UUID authorId, Boolean isPublic, int limit, int offset) {
    final int size = (limit > 0) ? limit : 20;
    final int page = Math.max(0, offset) / size;

    var pageable = PageRequest.of(page, size);
    var rows = jpa.findFiltered(authorId, isPublic, pageable);

    var items = rows.stream()
                    .map(ArticleMapper::toDomain)
                    .toList();


   Comparator<Article> byTitle =
    Comparator.<Article, String>comparing(
        a -> Optional.ofNullable(a.title()).orElse(""),
        String.CASE_INSENSITIVE_ORDER
    );

    // Bubble sort using your util (returns a NEW list)
    return SortUtils.bubbleSort(items, byTitle);
  }
  
  @Override
  public long count(UUID authorId, Boolean isPublic) {
    return jpa.countFiltered(authorId, isPublic);
  }

  @Override
  public Optional<Article> update(Article article) {
    if (!jpa.existsById(article.id())) {
      return Optional.empty();
    }
    var entity = toEntity(article);
    var updatedEntity = jpa.save(entity);
    return Optional.of(toDomain(updatedEntity));
  }

  @Override
  public boolean delete(UUID id) {
    if (!jpa.existsById(id)) {
      return false;
    }
    jpa.deleteById(id);
    return true;
  }

}
