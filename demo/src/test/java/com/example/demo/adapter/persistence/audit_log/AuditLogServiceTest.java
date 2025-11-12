package com.example.demo.adapter.persistence.audit_log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService Unit Tests")
class AuditLogServiceTest {

    @Mock
    private AuditLogJpaRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @Captor
    private ArgumentCaptor<AuditLogEntity> entityCaptor;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(repository, objectMapper);
    }

    @Test
    @DisplayName("logEvent saves audit log with all details")
    void logEvent_SavesAuditLog() throws JsonProcessingException {
        UUID actorUserId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        Map<String, Object> details = Map.of("key", "value");

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/articles");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(objectMapper.writeValueAsString(details)).thenReturn("{\"key\":\"value\"}");

        auditLogService.logEvent("CREATE", "Article", entityId, actorUserId, details, request);

        verify(repository).save(entityCaptor.capture());
        AuditLogEntity saved = entityCaptor.getValue();

        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getEntityType()).isEqualTo("Article");
        assertThat(saved.getEntityId()).isEqualTo(entityId.toString());
        assertThat(saved.getActorUserId()).isEqualTo(actorUserId);
        assertThat(saved.getMethod()).isEqualTo("POST");
        assertThat(saved.getPath()).isEqualTo("/api/v1/articles");
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(saved.getDetails()).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    @DisplayName("logEvent handles null request gracefully")
    void logEvent_WithNullRequest_SavesWithoutRequestDetails() throws JsonProcessingException {
        UUID actorUserId = UUID.randomUUID();
        Map<String, Object> details = Map.of("key", "value");

        when(objectMapper.writeValueAsString(details)).thenReturn("{\"key\":\"value\"}");

        auditLogService.logEvent("UPDATE", "User", "user-123", actorUserId, details, null);

        verify(repository).save(entityCaptor.capture());
        AuditLogEntity saved = entityCaptor.getValue();

        assertThat(saved.getAction()).isEqualTo("UPDATE");
        assertThat(saved.getEntityType()).isEqualTo("User");
        assertThat(saved.getMethod()).isNull();
        assertThat(saved.getPath()).isNull();
        assertThat(saved.getIpAddress()).isNull();
        assertThat(saved.getUserAgent()).isNull();
    }

    @Test
    @DisplayName("logEvent handles X-Forwarded-For header")
    void logEvent_WithXForwardedFor_UsesFirstIp() throws JsonProcessingException {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 192.168.1.1");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        auditLogService.logEvent("READ", "Article", UUID.randomUUID(), UUID.randomUUID(), Map.of(), request);

        verify(repository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getIpAddress()).isEqualTo("203.0.113.1");
    }

    @Test
    @DisplayName("logEvent handles X-Real-IP header")
    void logEvent_WithXRealIp_UsesHeader() throws JsonProcessingException {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.5");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        auditLogService.logEvent("DELETE", "User", UUID.randomUUID(), UUID.randomUUID(), Map.of(), request);

        verify(repository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getIpAddress()).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("logEvent handles JSON serialization failure")
    void logEvent_WithJsonError_DoesNotThrow() throws JsonProcessingException {
        when(request.getMethod()).thenReturn("POST");
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Error") {});

        // Should not throw exception
        auditLogService.logEvent("CREATE", "Article", UUID.randomUUID(), UUID.randomUUID(), Map.of("bad", new Object()), request);

        // Repository save should not be called due to exception
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("created() calls logEvent with CREATE action")
    void created_CallsLogEventWithCreateAction() throws JsonProcessingException {
        UUID entityId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Map<String, Object> details = Map.of("title", "Test");

        when(objectMapper.writeValueAsString(details)).thenReturn("{\"title\":\"Test\"}");

        auditLogService.created("Article", entityId, actorId, request, details);

        verify(repository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getAction()).isEqualTo("CREATE");
        assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("Article");
    }

    @Test
    @DisplayName("read() calls logEvent with READ action")
    void read_CallsLogEventWithReadAction() throws JsonProcessingException {
        UUID entityId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        auditLogService.read("User", entityId, actorId, request, Map.of());

        verify(repository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getAction()).isEqualTo("READ");
    }

    @Test
    @DisplayName("updated() calls logEvent with UPDATE action")
    void updated_CallsLogEventWithUpdateAction() throws JsonProcessingException {
        UUID entityId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        auditLogService.updated("Article", entityId, actorId, request, Map.of());

        verify(repository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getAction()).isEqualTo("UPDATE");
    }

    @Test
    @DisplayName("deleted() calls logEvent with DELETE action")
    void deleted_CallsLogEventWithDeleteAction() throws JsonProcessingException {
        UUID entityId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        auditLogService.deleted("User", entityId, actorId, request, Map.of());

        verify(repository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getAction()).isEqualTo("DELETE");
    }

    @Test
    @DisplayName("logEvent handles null entityId")
    void logEvent_WithNullEntityId_SavesNull() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        auditLogService.logEvent("LIST", "Article", null, UUID.randomUUID(), Map.of(), request);

        verify(repository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getEntityId()).isNull();
    }

    @Test
    @DisplayName("logEvent handles null details")
    void logEvent_WithNullDetails_SavesNull() {
        when(request.getMethod()).thenReturn("GET");

        auditLogService.logEvent("READ", "Article", UUID.randomUUID(), UUID.randomUUID(), null, request);

        verify(repository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getDetails()).isNull();
    }
}
