package com.bloodbridge.web;

import com.bloodbridge.domain.User;
import com.bloodbridge.service.RequestService;
import com.bloodbridge.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class RequestController {
    private static final String VIEW = "hasAnyAuthority('PERM_REQUEST_VIEW_OWN','PERM_REQUEST_VIEW_ALL')";
    private static final String MANAGE = "hasAnyAuthority('PERM_REQUEST_VIEW_OWN','PERM_REQUEST_MANAGE_ALL')";

    private final RequestService service;

    public RequestController(RequestService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_REQUEST_CREATE')")
    public RequestDetailDto create(@AuthenticationPrincipal User user, @Valid @RequestBody RequestCreate in) {
        Long id = service.create(user, in);   // queue runs after commit; re-read for fresh status
        return service.detail(id, user);
    }

    @GetMapping
    @PreAuthorize(VIEW)
    public List<RequestDto> list(@AuthenticationPrincipal User user) {
        return service.list(user);
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW)
    public RequestDetailDto detail(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return service.detail(id, user);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(MANAGE)
    public RequestDetailDto status(@AuthenticationPrincipal User user, @PathVariable Long id,
                                   @Valid @RequestBody StatusUpdate in) {
        service.changeStatus(id, user, in.status());
        return service.detail(id, user);
    }

    @PostMapping("/{id}/rematch")
    @PreAuthorize(MANAGE)
    public RequestDetailDto rematch(@AuthenticationPrincipal User user, @PathVariable Long id) {
        service.rematch(id, user);
        return service.detail(id, user);
    }
}
