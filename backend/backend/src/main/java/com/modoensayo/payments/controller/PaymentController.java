package com.modoensayo.payments.controller;

import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.modoensayo.payments.domain.CartItem;
import com.modoensayo.payments.domain.Enrollment;
import com.modoensayo.payments.dto.CartItemRequest;
import com.modoensayo.payments.dto.CheckoutRequest;
import com.modoensayo.payments.dto.CheckoutResponse;
import com.modoensayo.payments.dto.MercadoPagoPreferenceResponse;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.payments.service.MercadoPagoService;
import com.modoensayo.payments.service.PaymentService;
import com.modoensayo.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final MercadoPagoService mercadoPagoService;
    private final EnrollmentRepository enrollmentRepository;

    @PostMapping("/cart")
    public ResponseEntity<Void> addToCart(@Valid @RequestBody CartItemRequest request) {
        paymentService.addToCart(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/cart")
    public ResponseEntity<List<CartItem>> getCart() {
        return ResponseEntity.ok(paymentService.getCart(SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/cart")
    public ResponseEntity<Void> clearCart() {
        paymentService.clearCart(SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cart/{itemId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable String itemId) {
        paymentService.removeFromCart(SecurityUtils.getCurrentUserId(), itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(paymentService.checkout(SecurityUtils.getCurrentUserId(), request));
    }

    @PostMapping("/mercadopago/create-preference")
    public ResponseEntity<MercadoPagoPreferenceResponse> createMercadoPagoPreference() {
        try {
            return ResponseEntity.ok(mercadoPagoService.createPreference(SecurityUtils.getCurrentUserId()));
        } catch (MPException | MPApiException e) {
            log.error("Error al crear preferencia de Mercado Pago", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody Map<String, Object> payload) {
        String paymentId = mercadoPagoService.extractPaymentIdFromWebhook(payload);
        if (paymentId == null || paymentId.isBlank()) {
            log.warn("Webhook MP without payment id. payload={}", payload);
            return ResponseEntity.accepted().build();
        }

        try {
            mercadoPagoService.processWebhookPayment(paymentId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing MP webhook paymentId={}", paymentId, e);
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }
    }

    @GetMapping("/enrollments/class/{classId}")
    public ResponseEntity<List<Enrollment>> getEnrollmentsByClass(@PathVariable String classId) {
        return ResponseEntity.ok(enrollmentRepository.findByClassId(UUID.fromString(classId)));
    }
}
