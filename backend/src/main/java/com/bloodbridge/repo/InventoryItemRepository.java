package com.bloodbridge.repo;

import com.bloodbridge.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> findAllByOrderByBloodGroupAscComponentAsc();
}
