package com.modoensayo.payments.service;

import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.enums.ClassStatus;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.payments.domain.CartItem;
import com.modoensayo.payments.repository.CartItemRepository;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private CartItemRepository cartItemRepository;
    @Mock private ClassRepository classRepository;
    @Mock private EnrollmentRepository enrollmentRepository;

    @InjectMocks private PaymentService paymentService;

    private UUID ownerId;
    private UUID classId;
    private Class classEntity;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        classId = UUID.randomUUID();
        classEntity = Class.builder()
                .id(classId)
                .title("Cueca Básica")
                .price(15000.0)
                .status(ClassStatus.PUBLISHED)
                .build();
    }

    @Test
    void addToCart_shouldSaveCartItem_whenClassExists() {
        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(enrollmentRepository.existsByClassIdAndBeneficiaryId(classId, ownerId)).thenReturn(false);
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));

        paymentService.addToCart(ownerId, classId, "USER", ownerId);

        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addToCart_shouldThrow_whenClassNotFound() {
        when(classRepository.findById(classId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.addToCart(ownerId, classId, "USER", ownerId));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void getCart_shouldReturnItemsForOwner() {
        CartItem item = CartItem.builder().ownerId(ownerId).classId(classId).price(15000.0).build();
        when(cartItemRepository.findByOwnerId(ownerId)).thenReturn(List.of(item));

        List<CartItem> result = paymentService.getCart(ownerId);

        assertEquals(1, result.size());
        assertEquals(classId, result.get(0).getClassId());
    }

    @Test
    void removeFromCart_shouldDelete_whenOwnerMatches() {
        UUID itemId = UUID.randomUUID();
        CartItem item = CartItem.builder().ownerId(ownerId).build();
        item.setId(itemId);
        when(cartItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        paymentService.removeFromCart(ownerId, itemId);

        verify(cartItemRepository).deleteById(itemId);
    }

    @Test
    void removeFromCart_shouldThrow_whenOwnerMismatch() {
        UUID itemId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        CartItem item = CartItem.builder().ownerId(otherId).build();
        item.setId(itemId);
        when(cartItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.removeFromCart(ownerId, itemId));
        verify(cartItemRepository, never()).deleteById(any());
    }

    @Test
    void clearCart_shouldDeleteAllItemsForOwner() {
        paymentService.clearCart(ownerId);
        verify(cartItemRepository).deleteByOwnerId(ownerId);
    }
}
