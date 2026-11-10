package com.bloodbridge.repo;

import com.bloodbridge.domain.Role;
import com.bloodbridge.domain.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    /** Bulk delete runs immediately, so re-inserting the same pairs afterwards cannot hit the unique constraint. */
    @Modifying
    @Query("delete from RolePermission rp where rp.role = :role")
    void deleteAllForRole(@Param("role") Role role);
}
