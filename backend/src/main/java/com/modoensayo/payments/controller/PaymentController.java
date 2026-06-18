package com.modoensayo.payments.controller;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.payments.domain.CartItem;
import com.modoensayo.payments.service.MercadoPagoService;
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
    private final MercadoPagoService mercadoPagoService;

    @PostMapping("/cart")
    public ResponseEntity<Void> addToCart(@AuthenticationPrincipal CustomUserDetails user,
                                           @RequestBody Map<String, Object> body) {
        UUID classId = UUID.fromString(body.get("classId").toString());
        String beneficiaryType = body.get("beneficiaryType") != null ? body.get("beneficiaryType").toString() : "USER";
        UUID beneficiaryId = body.get("beneficiaryId") != null ? UUID.fromString(body.get("beneficiaryId").toString()) : null;
        paymentService.addToCart(user.getUserId(), classId, beneficiaryType, beneficiaryId);
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

    @PostMapping("/mercadopago/create-preference")
    public ResponseEntity<?> createPreference(@AuthenticationPrincipal CustomUserDetails user) {
        try {
            var response = mercadoPagoService.createPreference(user.getUserId().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error al crear preferencia: " + e.getMessage()));
        }
    }

    @GetMapping("/my-enrollments")
    public ResponseEntity<List<Map<String, Object>>> getMyEnrollments(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(paymentService.getMyEnrollments(user.getUserId()));
    }

    @GetMapping("/my-history")
    public ResponseEntity<List<Map<String, Object>>> getMyPaymentHistory(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(paymentService.getMyPaymentHistory(user.getUserId()));
    }
}
