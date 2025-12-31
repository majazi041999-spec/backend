package com.taskchi.taskchi.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskchi.taskchi.users.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository repo;
    private final ObjectMapper mapper;

    public AuditService(AuditLogRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public void log(User actor, String entityType, String entityId, String action,
                    Object before, Object after, HttpServletRequest request) {
        try {
            AuditLog l = new AuditLog();
            l.setActorUser(actor);
            l.setEntityType(entityType);
            l.setEntityId(entityId);
            l.setAction(action);
            l.setBeforeJson(before == null ? null : mapper.writeValueAsString(before));
            l.setAfterJson(after == null ? null : mapper.writeValueAsString(after));
            l.setIp(request.getRemoteAddr());
            l.setUserAgent(request.getHeader("User-Agent"));
            repo.save(l);
        } catch (Exception ignored) {
            // فعلاً لاگ اودیت نباید باعث fail شدن عملیات بشه
        }
    }
}
