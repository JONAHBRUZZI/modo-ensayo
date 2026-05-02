package com.modoensayo.payments.service;

import com.modoensayo.payments.domain.CartItem;
import com.modoensayo.payments.dto.CartItemRequest;
import com.modoensayo.payments.dto.CheckoutRequest;
import com.modoensayo.payments.dto.CheckoutResponse;
import com.modoensayo.payments.repository.CartItemRepository;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final CartItemRepository cartItemRepository;
    private final EnrollmentRepository enrollmentRepository;

    public PaymentService(CartItemRepository cartItemRepository,
                          EnrollmentRepository enrollmentRepository) {
        this.cartItemRepository = cartItemRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public void addToCart(String ownerId, CartItemRequest request) {
        UUID classId = UUID.fromString(request.classId());
        UUID beneficiaryId = request.beneficiaryId() != null ? UUID.fromString(request.beneficiaryId()) : null;

        if (enrollmentRepository.existsByClassIdAndBeneficiaryTypeAndBeneficiaryId(
                classId, request.beneficiaryType(), beneficiaryId)) {
            throw new BusinessException("Already enrolled in this class");
        }

        CartItem item = CartItem.builder()
                .ownerId(UUID.fromString(ownerId))
                .classId(classId)
                .beneficiaryType(request.beneficiaryType())
                .beneficiaryId(beneficiaryId)
                .build();

        cartItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCart(String ownerId) {
        return cartItemRepository.findByOwnerId(UUID.fromString(ownerId));
    }

    @Transactional
    public void removeFromCart(String ownerId, String itemId) {
        cartItemRepository.findById(UUID.fromString(itemId))
                .ifPresent(item -> {
                    if (!item.getOwnerId().equals(UUID.fromString(ownerId))) {
                        throw new ResourceNotFoundException("Cart item not found");
                    }
                    cartItemRepository.delete(item);
                });
    }

    @Transactional
    public void clearCart(String ownerId) {
        cartItemRepository.deleteByOwnerId(UUID.fromString(ownerId));
    }

    @Transactional
    public CheckoutResponse checkout(String ownerId, CheckoutRequest request) {
        throw new BusinessException("Checkout directo deshabilitado. Usa Mercado Pago.");
    }
}
