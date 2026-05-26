package com.modoensayo.payments.controller;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.payments.domain.CartItem;
import com.modoensayo.payments.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/cart")
    public ResponseEntity<Void> addToCart(@AuthenticationPrincipal CustomUserDetails user,
                                           @RequestBody Map<String, UUID> body) {
        paymentService.addToCart(user.getUserId(), body.get("classId"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cart")
    public ResponseEntity<Map<String, Object>> getCart(@AuthenticationPrincipal CustomUserDetails user) {
        List<CartItem> items = paymentService.getCart(user.getUserId());
        return ResponseEntity.ok(Map.of("items", items));
    }

    @DeleteMapping("/cart/{id}")
    public ResponseEntity<Void> removeFromCart(@AuthenticationPrincipal CustomUserDetails user,
                                                @PathVariable UUID id) {
        paymentService.removeFromCart(user.getUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(paymentService.checkout(user.getUserId()));
    }

    @PostMapping("/mercadopago/create-preference")
    public ResponseEntity<Map<String, Object>> createPreference(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(paymentService.createMercadoPagoPreference(user.getUserId()));
    }
}
