package com.modoensayo.payments.repository;

import com.modoensayo.payments.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByOwnerId(UUID ownerId);
    void deleteByOwnerId(UUID ownerId);
}
