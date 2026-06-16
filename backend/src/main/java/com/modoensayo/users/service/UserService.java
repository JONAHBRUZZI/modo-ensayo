package com.modoensayo.users.service;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ConflictException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.users.domain.*;
import com.modoensayo.users.dto.*;
import com.modoensayo.users.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final IdentityVerificationRepository identityVerificationRepository;
    private final RefundMethodRepository refundMethodRepository;
    private final ProfessionalProfileRepository professionalProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Cacheable(value = "userProfile", key = "#userDetails.userId")
    public UserProfileResponse getProfile(CustomUserDetails userDetails) {
        User user = getUser(userDetails.getUserId());
        IdentityVerification iv = identityVerificationRepository.findByUserId(user.getId()).orElse(null);
        return mapToProfile(user, iv);
    }

    @Transactional
    @CacheEvict(value = "userProfile", key = "#userDetails.userId")
    public UserProfileResponse updateProfile(CustomUserDetails userDetails, UpdateProfileRequest req) {
        User user = getUser(userDetails.getUserId());
        if (req.socialName() != null) user.setSocialName(req.socialName());
        if (req.phone() != null) user.setPhone(req.phone());
        user = userRepository.save(user);
        IdentityVerification iv = identityVerificationRepository.findByUserId(user.getId()).orElse(null);
        return mapToProfile(user, iv);
    }

    public List<RefundMethod> getRefundMethods(CustomUserDetails userDetails) {
        return refundMethodRepository.findByUserId(userDetails.getUserId());
    }

    @Transactional
    public RefundMethod createRefundMethod(CustomUserDetails userDetails, RefundMethodRequest req) {
        RefundMethod rm = RefundMethod.builder()
                .userId(userDetails.getUserId())
                .bank(req.bank())
                .accountType(req.accountType())
                .accountNumber(req.accountNumber())
                .accountHolder(req.accountHolder())
                .rut(req.rut())
                .build();
        return refundMethodRepository.save(rm);
    }

    @Transactional
    public void deleteRefundMethod(UUID userId, UUID id) {
        RefundMethod method = refundMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund method not found"));
        if (!method.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Refund method not found");
        }
        refundMethodRepository.delete(method);
    }

    @Transactional
    @CacheEvict(value = "userProfile", key = "#userDetails.userId")
    public UserProfileResponse setPreferredRefundMethod(CustomUserDetails userDetails, UUID methodId) {
        User user = getUser(userDetails.getUserId());
        user.setPreferredRefundMethodId(methodId);
        user = userRepository.save(user);
        IdentityVerification iv = identityVerificationRepository.findByUserId(user.getId()).orElse(null);
        return mapToProfile(user, iv);
    }

    public IdentityVerificationResponse getIdentityVerification(CustomUserDetails userDetails) {
        IdentityVerification iv = identityVerificationRepository.findByUserId(userDetails.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("No verification found"));
        return new IdentityVerificationResponse(iv.getId().toString(), iv.getUserId().toString(), iv.getDocumentUrl(), iv.getStatus(), null, iv.getCreatedAt());
    }

    @Transactional
    @CacheEvict(value = "userProfile", key = "#userDetails.userId")
    public IdentityVerificationResponse uploadIdentity(CustomUserDetails userDetails, String documentUrl, String documentType, String documentNumber, String fullName, java.time.LocalDate birthDate) {
        if ("RUT".equals(documentType) && documentNumber != null && !documentNumber.isBlank()) {
            if (!validarRutChileno(documentNumber)) {
                throw new BusinessException("El RUT ingresado no es valido. Verifica el digito verificador.");
            }
        }
        // R21: unicidad de documento entre cuentas — validacion exacta
        if (documentNumber != null && !documentNumber.isBlank()) {
            boolean duplicado = identityVerificationRepository
                    .existsByDocumentNumberAndStatusAndUserIdNot(documentNumber, "APPROVED", userDetails.getUserId());
            if (duplicado) {
                throw new ConflictException(
                        "Este documento ya esta asociado a otra cuenta. Contacta a soporte si crees que es un error.");
            }
        }
        // Fix #4: evitar que el mismo documento ya aprobado se registre en otra cuenta.
        // Se normaliza el documento (quita puntos, guiones, espacios) antes de comparar
        // para evitar falsos negativos por diferencias de formato.
        if (documentNumber != null && !documentNumber.isBlank()) {
            String normalizado = normalizarDocumento(documentNumber);

            // Caso 1: ya APROBADO en otra cuenta -> bloquear con mensaje especifico
            var aprobado = identityVerificationRepository
                    .findApprovedByNormalizedDocumentNumberExcludingUser(normalizado, userDetails.getUserId());
            if (aprobado.isPresent()) {
                String emailMascarado = userRepository.findById(aprobado.get().getUserId())
                        .map(u -> mascararEmail(u.getEmail()))
                        .orElse("otra cuenta");
                throw new BusinessException(
                        "Este " + (documentType != null ? documentType : "documento") + " (" + documentNumber + ") " +
                        "ya se encuentra VALIDADO en la cuenta " + emailMascarado + ". " +
                        "Si esa cuenta te pertenece, inicia sesion alli. Si crees que es un error, " +
                        "contacta al administrador para liberar el documento."
                );
            }

            // Caso 2: PENDIENTE en otra cuenta -> bloquear con mensaje informativo
            var pendiente = identityVerificationRepository
                    .findPendingByNormalizedDocumentNumberExcludingUser(normalizado, userDetails.getUserId());
            if (pendiente.isPresent()) {
                String emailMascarado = userRepository.findById(pendiente.get().getUserId())
                        .map(u -> mascararEmail(u.getEmail()))
                        .orElse("otra cuenta");
                throw new BusinessException(
                        "Este " + (documentType != null ? documentType : "documento") + " (" + documentNumber + ") " +
                        "esta actualmente EN REVISION en la cuenta " + emailMascarado + ". " +
                        "Espera a que se resuelva esa solicitud o contacta al administrador."
                );
            }
        }
        IdentityVerification iv = identityVerificationRepository.findByUserId(userDetails.getUserId())
                .orElse(IdentityVerification.builder().userId(userDetails.getUserId()).build());
        iv.setDocumentUrl(documentUrl);
        iv.setDocumentType(documentType);
        iv.setDocumentNumber(documentNumber);
        iv.setFullName(fullName);
        iv.setBirthDate(birthDate);
        iv.setStatus("PENDING");
        iv = identityVerificationRepository.save(iv);

        // Persistir estado PENDIENTE en User
        User user = userRepository.findById(userDetails.getUserId()).orElse(null);
        if (user != null) {
            user.setIdentidadEstado("PENDIENTE");
            user.setIdentidadValidada(false);
            userRepository.save(user);
        }

        return new IdentityVerificationResponse(iv.getId().toString(), iv.getUserId().toString(), iv.getDocumentUrl(), iv.getStatus(), null, iv.getCreatedAt(),
                iv.getDocumentType(), iv.getDocumentNumber(), iv.getFullName(), iv.getBirthDate());
    }

    /**
     * Normaliza un numero de documento para comparacion: quita puntos, guiones,
     * espacios y deja en minusculas. Ejemplo: "19.831.314-9" -> "198313149".
     */
    private String normalizarDocumento(String documento) {
        if (documento == null) return "";
        return documento.replaceAll("[.\\s\\-]", "").toLowerCase();
    }

    /**
     * Enmascara un email para mostrarlo en mensajes de error sin revelar la
     * cuenta completa. Ejemplo: "darlette@gmail.com" -> "d******e@gmail.com".
     */
    private String mascararEmail(String email) {
        if (email == null) return "otra cuenta";
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) return local.charAt(0) + "***" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    private boolean validarRutChileno(String rut) {
        String clean = rut.replaceAll("[^0-9kK]", "");
        if (clean.length() < 2) return false;
        String dv = clean.substring(clean.length() - 1).toUpperCase();
        String cuerpo = clean.substring(0, clean.length() - 1);
        int suma = 0;
        int multiplo = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * multiplo;
            multiplo = multiplo < 7 ? multiplo + 1 : 2;
        }
        int dvEsperado = 11 - (suma % 11);
        String dvCalculado = dvEsperado == 11 ? "0" : dvEsperado == 10 ? "K" : String.valueOf(dvEsperado);
        return dv.equals(dvCalculado);
    }

    @Transactional
    @CacheEvict(value = "userProfile", key = "#userId")
    public void deleteIdentityDocument(UUID userId) {
        IdentityVerification iv = identityVerificationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No verification found"));
        iv.setDocumentUrl(null);
        identityVerificationRepository.save(iv);
    }

    @Transactional
    public ProfessionalProfile saveProfessionalProfile(UUID userId, Map<String, Object> data) {
        User user = getUser(userId);
        ProfessionalProfile profile = professionalProfileRepository.findByUser_Id(userId)
                .orElse(ProfessionalProfile.builder().user(user).build());
        if (data.get("specialty") != null) profile.setSpecialty((String) data.get("specialty"));
        if (data.get("experienceYears") instanceof Number n) profile.setExperienceYears(n.intValue());
        if (data.get("description") != null) profile.setDescription((String) data.get("description"));
        if (data.get("especialidad") != null) profile.setEspecialidad((String) data.get("especialidad"));
        if (data.get("nivelEnsenanza") != null) profile.setNivelEnsenanza((String) data.get("nivelEnsenanza"));
        if (data.get("formacion") != null) profile.setFormacion((String) data.get("formacion"));
        if (data.get("instagram") != null) profile.setInstagram((String) data.get("instagram"));
        if (data.get("youtube") != null) profile.setYoutube((String) data.get("youtube"));
        if (data.get("sitioWeb") != null) profile.setSitioWeb((String) data.get("sitioWeb"));
        if (data.get("linkedin") != null) profile.setLinkedin((String) data.get("linkedin"));
        return professionalProfileRepository.save(profile);
    }

    @Transactional
    @CacheEvict(value = "userProfile", key = "#userId")
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        if (currentPassword == null || newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("La nueva contrasena debe tener al menos 6 caracteres");
        }
        User user = getUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException("La contrasena actual es incorrecta");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserProfileResponse mapToProfile(User user, IdentityVerification iv) {
        Set<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName()).collect(Collectors.toSet());
        boolean identidadValidada = iv != null && "APPROVED".equals(iv.getStatus());
        boolean identidadEnRevision = iv != null && "PENDING".equals(iv.getStatus());
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getSocialName(), user.getPhone(), user.getRut(), roles, user.isEnabled(),
                identidadValidada, identidadEnRevision, user.getPreferredRefundMethodId());
    }
}
