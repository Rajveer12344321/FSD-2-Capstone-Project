package com.bloodbridge.service;

import com.bloodbridge.domain.BloodComponent;
import com.bloodbridge.domain.BloodGroup;
import com.bloodbridge.domain.InventoryItem;
import com.bloodbridge.domain.User;
import com.bloodbridge.repo.InventoryItemRepository;
import com.bloodbridge.web.Dtos.InventoryDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Blood stock levels per group and component, with low-stock thresholds. Every change is audited. */
@Service
public class InventoryService {
    private final InventoryItemRepository repo;
    private final AuditService audit;

    public InventoryService(InventoryItemRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<InventoryDto> list() {
        return repo.findAllByOrderByBloodGroupAscComponentAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long lowStockCount() {
        return repo.findAll().stream().filter(i -> i.getUnits() <= i.getLowThreshold()).count();
    }

    @Transactional
    public InventoryDto adjust(User actor, Long id, int delta, String reason) {
        InventoryItem item = find(id);
        int updated = item.getUnits() + delta;
        if (updated < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot go below zero");
        int before = item.getUnits();
        item.setUnits(updated);
        item.setUpdatedAt(Instant.now());
        repo.save(item);
        audit.log(actor, "INVENTORY_ADJUST", item.getBloodGroup().label() + " " + item.getComponent(),
                before + " -> " + updated + " (" + reason + ")");
        return toDto(item);
    }

    @Transactional
    public InventoryDto setThreshold(User actor, Long id, int threshold) {
        InventoryItem item = find(id);
        item.setLowThreshold(threshold);
        item.setUpdatedAt(Instant.now());
        repo.save(item);
        audit.log(actor, "INVENTORY_THRESHOLD", item.getBloodGroup().label() + " " + item.getComponent(),
                "low-stock threshold " + threshold);
        return toDto(item);
    }

    /** Creates one row per group and component the first time the application starts. */
    @Transactional
    public void seedIfEmpty(boolean demoLevels) {
        if (repo.count() > 0) return;
        List<InventoryItem> rows = new ArrayList<>();
        for (BloodGroup g : BloodGroup.values()) {
            for (BloodComponent c : BloodComponent.values()) {
                InventoryItem i = new InventoryItem();
                i.setBloodGroup(g);
                i.setComponent(c);
                i.setUnits(demoLevels ? (g.ordinal() * 7 + c.ordinal() * 5) % 18 : 0);
                rows.add(i);
            }
        }
        repo.saveAll(rows);
    }

    private InventoryItem find(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock record not found"));
    }

    private InventoryDto toDto(InventoryItem i) {
        return new InventoryDto(i.getId(), i.getBloodGroup(), i.getComponent(), i.getUnits(), i.getLowThreshold(),
                i.getUnits() <= i.getLowThreshold(), i.getUpdatedAt());
    }
}
