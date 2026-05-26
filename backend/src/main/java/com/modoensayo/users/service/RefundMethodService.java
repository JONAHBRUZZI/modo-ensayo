package com.modoensayo.users.service;

import com.modoensayo.users.domain.RefundMethod;
import com.modoensayo.users.dto.RefundMethodRequest;
import com.modoensayo.users.repository.RefundMethodRepository;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundMethodService {

    private final RefundMethodRepository refundMethodRepository;

    @Transactional
    public RefundMethod create(String userId, RefundMethodRequest request) {
        RefundMethod method = RefundMethod.builder()
                .userId(UUID.fromString(userId))
                .bank(request.bank())
                .accountType(request.accountType())
                .accountNumber(request.accountNumber())
                .accountHolder(request.accountHolder())
                .rut(request.rut())
                .build();
        return refundMethodRepository.save(method);
    }

    @Transactional(readOnly = true)
    public List<RefundMethod> findByUser(String userId) {
        return refundMethodRepository.findByUserId(UUID.fromString(userId));
    }

    @Transactional
    public void delete(String userId, String methodId) {
        RefundMethod method = refundMethodRepository.findById(UUID.fromString(methodId))
                .orElseThrow(() -> new ResourceNotFoundException("Refund method not found"));
        if (!method.getUserId().equals(UUID.fromString(userId))) {
            throw new ResourceNotFoundException("Refund method not found");
        }
        refundMethodRepository.delete(method);
    }
}
