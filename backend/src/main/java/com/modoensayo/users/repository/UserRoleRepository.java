package com.modoensayo.users.repository;

import com.modoensayo.users.domain.UserRole;
import com.modoensayo.users.domain.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    List<UserRole> findByUser_Id(UUID userId);

    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.role.id = :roleId")
    long countByRoleId(@Param("roleId") Integer roleId);
}
