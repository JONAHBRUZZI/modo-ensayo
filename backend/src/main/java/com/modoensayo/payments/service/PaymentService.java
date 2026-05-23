package com.modoensayo.payments.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.payments.domain.CartItem;
import com.modoensayo.payments.repository.CartItemRepository;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final CartItemRepository cartItemRepository;
    private final ClassRepository classRepository;

    @Transactional
    public void addToCart(UUID ownerId, UUID classId) {
        Class c = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        CartItem item = CartItem.builder()
                .ownerId(ownerId).classId(classId)
                .classTitle(c.getTitle()).discipline(c.getDiscipline() != null ? c.getDiscipline().name() : null)
                .level(c.getLevel() != null ? c.getLevel().name() : null).price(c.getPrice())
                .build();
        cartItemRepository.save(item);
    }

    public List<CartItem> getCart(UUID ownerId) {
        return cartItemRepository.findByOwnerId(ownerId);
    }

    @Transactional
    public void removeFromCart(UUID itemId) {
        cartItemRepository.deleteById(itemId);
    }

    @Transactional
    public void clearCart(UUID ownerId) {
        cartItemRepository.deleteByOwnerId(ownerId);
    }

    @Transactional
    public Map<String, Object> checkout(UUID ownerId) {
        List<CartItem> items = cartItemRepository.findByOwnerId(ownerId);
        cartItemRepository.deleteByOwnerId(ownerId);
        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", items.stream().mapToDouble(CartItem::getPrice).sum());
        return result;
    }

    public Map<String, Object> createMercadoPagoPreference(UUID ownerId) {
        List<CartItem> items = cartItemRepository.findByOwnerId(ownerId);
        double total = items.stream().mapToDouble(CartItem::getPrice).sum();
        String prefId = UUID.randomUUID().toString().substring(0, 16);

        Map<String, Object> resp = new HashMap<>();
        resp.put("preferenceId", prefId);
        resp.put("initPoint", "/payment/pending");
        resp.put("total", total);
        resp.put("items", items);
        return resp;
    }
}
