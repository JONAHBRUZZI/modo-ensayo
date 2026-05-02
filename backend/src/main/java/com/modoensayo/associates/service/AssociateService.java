package com.modoensayo.associates.service;

import com.modoensayo.associates.domain.Associate;
import com.modoensayo.associates.dto.AssociateRequest;
import com.modoensayo.associates.dto.AssociateResponse;
import com.modoensayo.associates.repository.AssociateRepository;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AssociateService {

    private final AssociateRepository associateRepository;

    public AssociateService(AssociateRepository associateRepository) {
        this.associateRepository = associateRepository;
    }

    @Transactional
    public AssociateResponse create(String ownerId, AssociateRequest request) {
        Associate associate = Associate.builder()
                .ownerId(UUID.fromString(ownerId))
                .name(request.name())
                .relation(request.relation())
                .birthDate(request.birthDate())
                .rut(request.rut())
                .build();

        associateRepository.save(associate);

        return toResponse(associate);
    }

    @Transactional(readOnly = true)
    public List<AssociateResponse> findByOwner(String ownerId) {
        return associateRepository.findByOwnerId(UUID.fromString(ownerId)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(String ownerId, String associateId) {
        Associate associate = associateRepository.findById(UUID.fromString(associateId))
                .orElseThrow(() -> new ResourceNotFoundException("Associate not found"));

        if (!associate.getOwnerId().equals(UUID.fromString(ownerId))) {
            throw new ResourceNotFoundException("Associate not found");
        }

        associateRepository.delete(associate);
    }

    private AssociateResponse toResponse(Associate associate) {
        return new AssociateResponse(
                associate.getId().toString(),
                associate.getName(),
                associate.getRelation(),
                associate.getBirthDate(),
                associate.getRut()
        );
    }
}
