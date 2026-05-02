package com.modoensayo.payments.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.payments.domain.CartItem;
import com.modoensayo.payments.domain.Enrollment;
import com.modoensayo.payments.domain.Payment;
import com.modoensayo.payments.dto.CartItemRequest;
import com.modoensayo.payments.dto.CheckoutRequest;
import com.modoensayo.payments.dto.CheckoutResponse;
import com.modoensayo.payments.enums.PaymentStatus;
import com.modoensayo.payments.repository.CartItemRepository;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.payments.repository.PaymentRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final CartItemRepository cartItemRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final ClassRepository classRepository;

    public PaymentService(CartItemRepository cartItemRepository,
                          EnrollmentRepository enrollmentRepository,
                          PaymentRepository paymentRepository,
                          ClassRepository classRepository) {
        this.cartItemRepository = cartItemRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.paymentRepository = paymentRepository;
        this.classRepository = classRepository;
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
    public CheckoutResponse checkout(String ownerId, CheckoutRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByOwnerId(UUID.fromString(ownerId));

        if (cartItems.isEmpty()) {
            throw new BusinessException("Cart is empty");
        }

        List<CheckoutResponse.EnrollmentItem> enrollments = new ArrayList<>();
        int totalAmount = 0;

        for (CartItem item : cartItems) {
            Class classEntity = classRepository.findById(item.getClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

            Enrollment enrollment = Enrollment.builder()
                    .classId(item.getClassId())
                    .beneficiaryType(item.getBeneficiaryType())
                    .beneficiaryId(item.getBeneficiaryId())
                    .build();

            enrollmentRepository.save(enrollment);

            Payment payment = Payment.builder()
                    .enrollment(enrollment)
                    .amount(classEntity.getPrice())
                    .status(PaymentStatus.RETAINED)
                    .build();

            paymentRepository.save(payment);

            totalAmount += classEntity.getPrice();
            enrollments.add(new CheckoutResponse.EnrollmentItem(
                    enrollment.getId().toString(),
                    item.getClassId().toString(),
                    classEntity.getPrice()
            ));
        }

        cartItemRepository.deleteByOwnerId(UUID.fromString(ownerId));

        return new CheckoutResponse(
                UUID.randomUUID().toString(),
                totalAmount,
                PaymentStatus.RETAINED.name(),
                enrollments
        );
    }
}
