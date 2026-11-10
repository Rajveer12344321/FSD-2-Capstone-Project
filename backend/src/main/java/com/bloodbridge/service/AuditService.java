package com.bloodbridge.service;

import com.bloodbridge.domain.AuditLog;
import com.bloodbridge.domain.User;
import com.bloodbridge.repo.AuditLogRepository;
import com.bloodbridge.web.Dtos.AuditDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Append-only record of security-relevant actions (logins, role changes, verification, permission edits). */
@Service
public class AuditService {
    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void log(User actor, String action, String target, String detail) {
        logAs(actor == null ? "anonymous" : actor.getEmail(), action, target, detail);
    }

    public void logAs(String actorEmail, String action, String target, String detail) {
        AuditLog a = new AuditLog();
        a.setActor(cut(actorEmail, 150));
        a.setAction(cut(action, 60));
        a.setTarget(cut(target, 150));
        a.setDetail(cut(detail, 500));
        repo.save(a);
    }

    @Transactional(readOnly = true)
    public List<AuditDto> recent() {
        return repo.findTop100ByOrderByCreatedAtDesc().stream()
                .map(a -> new AuditDto(a.getId(), a.getActor(), a.getAction(), a.getTarget(), a.getDetail(), a.getCreatedAt()))
                .toList();
    }

    private String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
