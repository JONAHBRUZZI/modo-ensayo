package com.modoensayo.associates.repository;

import com.modoensayo.associates.domain.Associate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssociateRepository extends JpaRepository<Associate, UUID> {
    List<Associate> findByOwnerId(UUID ownerId);
}
