package com.example.demo.adapter.web;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.adapter.persistence.audit_log.AuditLogService;
import com.example.demo.domain.model.ApiResponse;
import com.example.demo.domain.model.Article;
import com.example.demo.domain.port.in.ArticleUseCase;
import com.example.demo.security.AuthorizationService;
import com.example.demo.security.UserPrincipal;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/articles")
public class ArticleController {
  private final ArticleUseCase useCase;
  private final AuditLogService audit;
  private final AuthorizationService authz;

  public ArticleController(ArticleUseCase useCase, AuditLogService audit, AuthorizationService authz) {
    this.useCase = useCase;
    this.audit = audit;
    this.authz = authz;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<Article>> create(
      @RequestBody CreateArticleBody body,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest req
  ) {
    // Check if user has permission to create articles (CONTRIBUTOR or higher)
    authz.requireCanCreateArticles();

    // Use authenticated user as author (ignore body.authorId for security)
    UUID authorId = authz.getCurrentUserId();

    Article toCreate = new Article(
        null, body.title(), body.content(), authorId, body.isPublic(), null, null
    );
    Article created = useCase.create(toCreate);

    audit.created(
        "Article",
        created.id(),
        authz.getCurrentUserId(), // Use logged-in user ID
        req,
        Map.of("title", created.title(), "isPublic", created.isPublic())
    );

    return ResponseEntity
        .created(URI.create("/articles/" + created.id()))
        .body(ApiResponse.created("Article created", created));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<Article>> get(
      @PathVariable UUID id,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest req
  ) {
    var res = useCase.getById(id);
    if (res.isEmpty()) {
      return ResponseEntity.status(404).body(ApiResponse.error(404, "Not Found", "Article not found"));
    }

    Article article = res.get();

    // Check if user has permission to view this article
    // All users must be authenticated now
    try {
      authz.requireCanViewArticle(article);
    } catch (AccessDeniedException e) {
      return ResponseEntity.status(403).body(ApiResponse.error(403, "Forbidden", e.getMessage()));
    }

    audit.read("Article", id, authz.getCurrentUserId(), req, Map.of("hit", true, "articleAuthorId", article.authorId().toString()));

    return ResponseEntity.ok(ApiResponse.ok("OK", article));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<Article>>> list(
      @RequestParam(required = false) UUID authorId,
      @RequestParam(required = false) Boolean isPublic,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int offset,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest req
  ) {
    List<Article> items = useCase.list(authorId, isPublic, limit, offset);

    // Filter articles based on user permissions
    // VIEWER: only public articles
    // CONTRIBUTOR/EDITOR/ADMIN: all articles
    List<Article> filteredItems = items.stream()
        .filter(article -> authz.canViewArticle(article))
        .collect(Collectors.toList());

    audit.logEvent("LIST", "Article", null, authz.getCurrentUserId(),
        Map.of("count", filteredItems.size(), "limit", limit, "offset", offset), req);

    return ResponseEntity.ok(ApiResponse.ok("OK", filteredItems));
  }


  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<Article>> update(
      @PathVariable UUID id,
      @RequestBody UpdateArticleBody body,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest req
  ) {
    // First, check if article exists
    var existingOpt = useCase.getById(id);
    if (existingOpt.isEmpty()) {
      return ResponseEntity.status(404).body(ApiResponse.error(404, "Not Found", "Article not found"));
    }

    Article existing = existingOpt.get();

    // Check if user has permission to update this article
    try {
      authz.requireCanUpdateArticle(existing);
    } catch (AccessDeniedException e) {
      return ResponseEntity.status(403).body(ApiResponse.error(403, "Forbidden", e.getMessage()));
    }

    // Perform update
    var updated = useCase.update(new Article(
        id, body.title(), body.content(), null, body.isPublic(), null, null
    ));

    if (updated.isEmpty()) {
      return ResponseEntity.status(404).body(ApiResponse.error(404, "Not Found", "Article not found"));
    }

    var art = updated.get();
    audit.updated(
        "Article",
        id,
        authz.getCurrentUserId(),
        req,
        Map.of("title", art.title(), "isPublic", art.isPublic())
    );

    return ResponseEntity.ok(ApiResponse.ok("Article updated", art));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> delete(
      @PathVariable UUID id,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest req
  ) {
    // First, check if article exists
    var existingOpt = useCase.getById(id);
    if (existingOpt.isEmpty()) {
      return ResponseEntity.status(404).body(ApiResponse.error(404, "Not Found", "Article not found"));
    }

    Article existing = existingOpt.get();

    // Check if user has permission to delete this article
    // Only SUPER_ADMIN or EDITOR (who owns the article) can delete
    try {
      authz.requireCanDeleteArticle(existing);
    } catch (AccessDeniedException e) {
      return ResponseEntity.status(403).body(ApiResponse.error(403, "Forbidden", e.getMessage()));
    }

    // Perform delete
    boolean ok = useCase.delete(id);
    if (!ok) {
      return ResponseEntity.status(404).body(ApiResponse.error(404, "Not Found", "Article not found"));
    }

    audit.deleted("Article", id, authz.getCurrentUserId(), req, Map.of("deleted", true));
    return ResponseEntity.ok(ApiResponse.noContent("Deleted"));
  }

  // DTOs
  record CreateArticleBody(String title, String content, Boolean isPublic) {}
  record UpdateArticleBody(String title, String content, Boolean isPublic) {}
}