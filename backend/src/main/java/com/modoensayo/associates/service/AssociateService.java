package com.modoensayo.associates.service;

import com.modoensayo.associates.domain.Associate;
import com.modoensayo.associates.dto.AssociateRequest;
import com.modoensayo.associates.dto.AssociateResponse;
import com.modoensayo.associates.repository.AssociateRepository;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssociateService {

    private final AssociateRepository associateRepository;

    public List<AssociateResponse> getByOwner(UUID ownerId) {
        return associateRepository.findByOwnerId(ownerId).stream()
                .map(a -> new AssociateResponse(a.getId(), a.getEmail(), a.getStatus(), a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public AssociateResponse create(UUID ownerId, AssociateRequest req) {
        Associate a = Associate.builder().ownerId(ownerId).email(req.email()).build();
        a = associateRepository.save(a);
        return new AssociateResponse(a.getId(), a.getEmail(), a.getStatus(), a.getCreatedAt());
    }

    @Transactional
    public void delete(UUID ownerId, UUID id) {
        Associate a = associateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Associate not found"));
        if (!a.getOwnerId().equals(ownerId)) {
            throw new ResourceNotFoundException("Associate not found");
        }
        associateRepository.deleteById(id);
    }
}
