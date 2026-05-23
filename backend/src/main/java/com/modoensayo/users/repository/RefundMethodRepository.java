package com.modoensayo.users.repository;

import com.modoensayo.users.domain.RefundMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RefundMethodRepository extends JpaRepository<RefundMethod, UUID> {
    List<RefundMethod> findByUserId(UUID userId);
}
