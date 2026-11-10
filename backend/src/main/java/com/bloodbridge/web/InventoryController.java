package com.bloodbridge.web;

import com.bloodbridge.domain.User;
import com.bloodbridge.service.InventoryService;
import com.bloodbridge.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_INVENTORY_VIEW')")
    public List<InventoryDto> list() {
        return service.list();
    }

    @PatchMapping("/{id}/adjust")
    @PreAuthorize("hasAuthority('PERM_INVENTORY_MANAGE')")
    public InventoryDto adjust(@AuthenticationPrincipal User actor, @PathVariable Long id,
                               @Valid @RequestBody InventoryAdjust in) {
        return service.adjust(actor, id, in.delta(), in.reason());
    }

    @PatchMapping("/{id}/threshold")
    @PreAuthorize("hasAuthority('PERM_INVENTORY_MANAGE')")
    public InventoryDto threshold(@AuthenticationPrincipal User actor, @PathVariable Long id,
                                  @Valid @RequestBody ThresholdUpdate in) {
        return service.setThreshold(actor, id, in.lowThreshold());
    }
}
