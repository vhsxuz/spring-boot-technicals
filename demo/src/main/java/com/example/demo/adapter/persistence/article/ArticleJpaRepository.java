package com.example.demo.adapter.persistence.article;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleJpaRepository extends JpaRepository<ArticleEntity, UUID> {

  // Flexible list with optional filters + pagination
  @Query("""
         select e
         from ArticleEntity e
         where (:authorId is null or e.authorId = :authorId)
           and (:isPublic is null or e.isPublic = :isPublic)
         order by e.createdAt desc
         """)
  List<ArticleEntity> findFiltered(@Param("authorId") UUID authorId,
                                   @Param("isPublic") Boolean isPublic,
                                   Pageable pageable);

  // Flexible count with optional filters
  @Query("""
         select count(e)
         from ArticleEntity e
         where (:authorId is null or e.authorId = :authorId)
           and (:isPublic is null or e.isPublic = :isPublic)
         """)
  long countFiltered(@Param("authorId") UUID authorId,
                     @Param("isPublic") Boolean isPublic);
}
