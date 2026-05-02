package com.modoensayo.payments.controller;

import com.modoensayo.payments.domain.CartItem;
import com.modoensayo.payments.dto.CartItemRequest;
import com.modoensayo.payments.dto.CheckoutRequest;
import com.modoensayo.payments.dto.CheckoutResponse;
import com.modoensayo.payments.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/cart")
    public ResponseEntity<Void> addToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CartItemRequest request) {
        paymentService.addToCart(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/cart")
    public ResponseEntity<List<CartItem>> getCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(paymentService.getCart(userDetails.getUsername()));
    }

    @DeleteMapping("/cart")
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cart/{itemId}")
    public ResponseEntity<Void> removeFromCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String itemId) {
        paymentService.removeFromCart(userDetails.getUsername(), itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(paymentService.checkout(userDetails.getUsername(), request));
    }
}
