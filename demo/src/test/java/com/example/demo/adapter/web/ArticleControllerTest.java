package com.example.demo.adapter.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.adapter.persistence.audit_log.AuditLogService;
import com.example.demo.domain.model.Article;
import com.example.demo.domain.port.in.ArticleUseCase;
import com.example.demo.security.AuthorizationService;
import com.example.demo.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(ArticleController.class)
@DisplayName("ArticleController Unit Tests")
class ArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ArticleUseCase articleUseCase;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private AuthorizationService authorizationService;

    private UserPrincipal createPrincipal(UUID userId, String email, Set<String> roles) {
        return new UserPrincipal(userId, email, "hashedPassword", roles);
    }

    // ========== CREATE Tests ==========

    @Test
    @DisplayName("POST /articles - Success as CONTRIBUTOR")
    @WithMockUser
    void createArticle_AsContributor_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId, "user@example.com", Set.of("CONTRIBUTOR"));

        when(authorizationService.getCurrentUserId()).thenReturn(userId);

        Article createdArticle = new Article(
                articleId, "Test Title", "Test Content", userId, true,
                OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(articleUseCase.create(any(Article.class))).thenReturn(createdArticle);

        String requestBody = """
                {
                    "title": "Test Title",
                    "content": "Test Content",
                    "isPublic": true
                }
                """;

        mockMvc.perform(post("/articles")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value(articleId.toString()))
                .andExpect(jsonPath("$.data.title").value("Test Title"));

        verify(authorizationService).requireCanCreateArticles();
        verify(articleUseCase).create(any(Article.class));
        verify(auditLogService).created(eq("Article"), eq(articleId), eq(userId), any(), any());
    }

    @Test
    @DisplayName("POST /articles - Forbidden for VIEWER")
    @WithMockUser
    void createArticle_AsViewer_Forbidden() throws Exception {
        UserPrincipal principal = createPrincipal(UUID.randomUUID(), "user@example.com", Set.of("VIEWER"));

        doThrow(new AccessDeniedException("Insufficient permissions to create articles"))
                .when(authorizationService).requireCanCreateArticles();

        String requestBody = """
                {
                    "title": "Test Title",
                    "content": "Test Content",
                    "isPublic": true
                }
                """;

        mockMvc.perform(post("/articles")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        verify(authorizationService).requireCanCreateArticles();
        verify(articleUseCase, never()).create(any(Article.class));
    }

    // ========== READ Tests ==========

    @Test
    @DisplayName("GET /articles/{id} - Success")
    @WithMockUser
    void getArticle_Success() throws Exception {
        UUID articleId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(viewerId, "user@example.com", Set.of("CONTRIBUTOR"));

        Article article = new Article(
                articleId, "Test Title", "Test Content", authorId, true,
                OffsetDateTime.now(), OffsetDateTime.now()
        );

        when(articleUseCase.getById(articleId)).thenReturn(Optional.of(article));
        when(authorizationService.getCurrentUserId()).thenReturn(viewerId);

        mockMvc.perform(get("/articles/" + articleId)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(articleId.toString()))
                .andExpect(jsonPath("$.data.title").value("Test Title"));

        verify(authorizationService).requireCanViewArticle(article);
        verify(auditLogService).read(eq("Article"), eq(articleId), eq(viewerId), any(), any());
    }

    @Test
    @DisplayName("GET /articles/{id} - Not Found")
    @WithMockUser
    void getArticle_NotFound() throws Exception {
        UUID articleId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(UUID.randomUUID(), "user@example.com", Set.of("CONTRIBUTOR"));

        when(articleUseCase.getById(articleId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/articles/" + articleId)
                        .with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        verify(authorizationService, never()).requireCanViewArticle(any());
    }

    @Test
    @DisplayName("GET /articles/{id} - Forbidden")
    @WithMockUser
    void getArticle_Forbidden() throws Exception {
        UUID articleId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(UUID.randomUUID(), "user@example.com", Set.of("VIEWER"));

        Article article = new Article(
                articleId, "Test Title", "Test Content", UUID.randomUUID(), false,
                OffsetDateTime.now(), OffsetDateTime.now()
        );

        when(articleUseCase.getById(articleId)).thenReturn(Optional.of(article));
        doThrow(new AccessDeniedException("You don't have permission to view this article"))
                .when(authorizationService).requireCanViewArticle(article);

        mockMvc.perform(get("/articles/" + articleId)
                        .with(user(principal)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    // ========== LIST Tests ==========

    @Test
    @DisplayName("GET /articles - Success with filtering")
    @WithMockUser
    void listArticles_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId, "user@example.com", Set.of("CONTRIBUTOR"));

        Article article1 = new Article(UUID.randomUUID(), "Title 1", "Content 1", userId, true, null, null);
        Article article2 = new Article(UUID.randomUUID(), "Title 2", "Content 2", userId, false, null, null);

        when(articleUseCase.list(null, null, 20, 0)).thenReturn(List.of(article1, article2));
        when(authorizationService.canViewArticle(any())).thenReturn(true);
        when(authorizationService.getCurrentUserId()).thenReturn(userId);

        mockMvc.perform(get("/articles")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(auditLogService).logEvent(eq("LIST"), eq("Article"), eq(null), eq(userId), any(), any());
    }

    @Test
    @DisplayName("GET /articles - Filters out unauthorized articles")
    @WithMockUser
    void listArticles_FiltersUnauthorized() throws Exception {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId, "user@example.com", Set.of("VIEWER"));

        Article publicArticle = new Article(UUID.randomUUID(), "Public", "Content", userId, true, null, null);
        Article privateArticle = new Article(UUID.randomUUID(), "Private", "Content", UUID.randomUUID(), false, null, null);

        when(articleUseCase.list(null, null, 20, 0)).thenReturn(List.of(publicArticle, privateArticle));
        when(authorizationService.canViewArticle(publicArticle)).thenReturn(true);
        when(authorizationService.canViewArticle(privateArticle)).thenReturn(false);
        when(authorizationService.getCurrentUserId()).thenReturn(userId);

        mockMvc.perform(get("/articles")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Public"));
    }

    // ========== UPDATE Tests ==========

    @Test
    @DisplayName("PATCH /articles/{id} - Success as owner")
    @WithMockUser
    void updateArticle_AsOwner_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId, "user@example.com", Set.of("CONTRIBUTOR"));

        Article existing = new Article(articleId, "Old Title", "Old Content", userId, true, null, null);
        Article updated = new Article(articleId, "New Title", "New Content", userId, true, null, null);

        when(articleUseCase.getById(articleId)).thenReturn(Optional.of(existing));
        when(articleUseCase.update(any(Article.class))).thenReturn(Optional.of(updated));
        when(authorizationService.getCurrentUserId()).thenReturn(userId);

        String requestBody = """
                {
                    "title": "New Title",
                    "content": "New Content",
                    "isPublic": true
                }
                """;

        mockMvc.perform(patch("/articles/" + articleId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("New Title"));

        verify(authorizationService).requireCanUpdateArticle(existing);
        verify(auditLogService).updated(eq("Article"), eq(articleId), eq(userId), any(), any());
    }

    @Test
    @DisplayName("PATCH /articles/{id} - Forbidden for non-owner")
    @WithMockUser
    void updateArticle_AsNonOwner_Forbidden() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId, "user@example.com", Set.of("CONTRIBUTOR"));

        Article existing = new Article(articleId, "Title", "Content", UUID.randomUUID(), true, null, null);

        when(articleUseCase.getById(articleId)).thenReturn(Optional.of(existing));
        doThrow(new AccessDeniedException("You don't have permission to update this article"))
                .when(authorizationService).requireCanUpdateArticle(existing);

        String requestBody = """
                {
                    "title": "New Title",
                    "content": "New Content",
                    "isPublic": true
                }
                """;

        mockMvc.perform(patch("/articles/" + articleId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        verify(articleUseCase, never()).update(any());
    }

    // ========== DELETE Tests ==========

    @Test
    @DisplayName("DELETE /articles/{id} - Success as EDITOR owner")
    @WithMockUser
    void deleteArticle_AsEditorOwner_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId, "user@example.com", Set.of("EDITOR"));

        Article existing = new Article(articleId, "Title", "Content", userId, true, null, null);

        when(articleUseCase.getById(articleId)).thenReturn(Optional.of(existing));
        when(articleUseCase.delete(articleId)).thenReturn(true);
        when(authorizationService.getCurrentUserId()).thenReturn(userId);

        mockMvc.perform(delete("/articles/" + articleId)
                        .with(user(principal)))
                .andExpect(status().isOk());

        verify(authorizationService).requireCanDeleteArticle(existing);
        verify(auditLogService).deleted(eq("Article"), eq(articleId), eq(userId), any(), any());
    }

    @Test
    @DisplayName("DELETE /articles/{id} - Forbidden for CONTRIBUTOR")
    @WithMockUser
    void deleteArticle_AsContributor_Forbidden() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId, "user@example.com", Set.of("CONTRIBUTOR"));

        Article existing = new Article(articleId, "Title", "Content", userId, true, null, null);

        when(articleUseCase.getById(articleId)).thenReturn(Optional.of(existing));
        doThrow(new AccessDeniedException("You don't have permission to delete this article"))
                .when(authorizationService).requireCanDeleteArticle(existing);

        mockMvc.perform(delete("/articles/" + articleId)
                        .with(user(principal)))
                .andExpect(status().isForbidden());

        verify(articleUseCase, never()).delete(any());
    }

    @Test
    @DisplayName("DELETE /articles/{id} - Not Found")
    @WithMockUser
    void deleteArticle_NotFound() throws Exception {
        UUID articleId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(UUID.randomUUID(), "user@example.com", Set.of("EDITOR"));

        when(articleUseCase.getById(articleId)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/articles/" + articleId)
                        .with(user(principal)))
                .andExpect(status().isNotFound());

        verify(articleUseCase, never()).delete(any());
    }
}
