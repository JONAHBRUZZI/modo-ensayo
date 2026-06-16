package com.modoensayo.users.service;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ConflictException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.users.domain.*;
import com.modoensayo.users.dto.*;
import com.modoensayo.users.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.*;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private IdentityVerificationRepository identityVerificationRepository;
    @Mock private RefundMethodRepository refundMethodRepository;
    @Mock private ProfessionalProfileRepository professionalProfileRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    private UUID userId;
    private User user;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("test@test.com")
                .fullName("Test User")
                .passwordHash("hashed")
                .userRoles(new HashSet<>())
                .build();
        userDetails = new CustomUserDetails(userId, "test@test.com", "hashed", "Test User", true, Set.of());
    }

    // --- getProfile ---

    @Test
    void getProfile_shouldReturnProfile_whenUserExists() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(identityVerificationRepository.findByUserId(userId)).thenReturn(Optional.empty());

        UserProfileResponse profile = userService.getProfile(userDetails);

        assertNotNull(profile);
        assertEquals(userId, profile.id());
        assertEquals("test@test.com", profile.email());
        assertFalse(profile.identidadValidada());
    }

    @Test
    void getProfile_shouldMarkIdentidadValidada_whenApproved() {
        IdentityVerification iv = IdentityVerification.builder()
                .id(UUID.randomUUID()).userId(userId).status("APPROVED").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(identityVerificationRepository.findByUserId(userId)).thenReturn(Optional.of(iv));

        UserProfileResponse profile = userService.getProfile(userDetails);

        assertTrue(profile.identidadValidada());
        assertFalse(profile.identidadEnRevision());
    }

    @Test
    void getProfile_shouldMarkIdentidadEnRevision_whenPending() {
        IdentityVerification iv = IdentityVerification.builder()
                .id(UUID.randomUUID()).userId(userId).status("PENDING").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(identityVerificationRepository.findByUserId(userId)).thenReturn(Optional.of(iv));

        UserProfileResponse profile = userService.getProfile(userDetails);

        assertFalse(profile.identidadValidada());
        assertTrue(profile.identidadEnRevision());
    }

    // --- updateProfile ---

    @Test
    void updateProfile_shouldUpdateSocialName() {
        UpdateProfileRequest req = new UpdateProfileRequest("NombreSocial", null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(identityVerificationRepository.findByUserId(userId)).thenReturn(Optional.empty());

        UserProfileResponse result = userService.updateProfile(userDetails, req);

        verify(userRepository).save(argThat(u -> "NombreSocial".equals(u.getSocialName())));
        assertNotNull(result);
    }

    // --- changePassword ---

    @Test
    void changePassword_shouldSucceed_withCorrectCurrentPassword() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("currentPass", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("newHashed");

        assertDoesNotThrow(() -> userService.changePassword(userId, "currentPass", "newPass123"));
        verify(userRepository).save(argThat(u -> "newHashed".equals(u.getPasswordHash())));
    }

    @Test
    void changePassword_shouldThrow_withWrongCurrentPassword() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "hashed")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> userService.changePassword(userId, "wrongPass", "newPass123"));
    }

    @Test
    void changePassword_shouldThrow_whenNewPasswordTooShort() {
        assertThrows(BusinessException.class,
                () -> userService.changePassword(userId, "currentPass", "abc"));
    }

    // --- uploadIdentity (RUT validation y unicidad) ---

    @Test
    void uploadIdentity_shouldThrow_withInvalidRut() {
        // RUT con dígito verificador incorrecto
        assertThrows(BusinessException.class, () ->
                userService.uploadIdentity(userDetails, "url", "RUT", "12.345.678-0",
                        "Test User", LocalDate.of(1990, 1, 1)));
    }

    @Test
    void uploadIdentity_shouldThrow_whenDocumentAlreadyApprovedInAnotherAccount() {
        IdentityVerification other = IdentityVerification.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID()).status("APPROVED").build();
        User otherUser = User.builder().id(other.getUserId()).email("otro@test.com").build();

        when(identityVerificationRepository.existsByDocumentNumberAndStatusAndUserIdNot(
                anyString(), eq("APPROVED"), eq(userId))).thenReturn(false);
        when(identityVerificationRepository.findApprovedByNormalizedDocumentNumberExcludingUser(
                anyString(), eq(userId))).thenReturn(Optional.of(other));
        when(userRepository.findById(other.getUserId())).thenReturn(Optional.of(otherUser));

        assertThrows(BusinessException.class, () ->
                userService.uploadIdentity(userDetails, "url", "PASAPORTE", "AB123456",
                        "Test User", LocalDate.of(1990, 1, 1)));
    }

    @Test
    void uploadIdentity_shouldSucceed_withValidDocument() {
        IdentityVerification saved = IdentityVerification.builder()
                .id(UUID.randomUUID()).userId(userId).status("PENDING")
                .documentType("PASAPORTE").documentNumber("AB999999")
                .fullName("Test User").documentUrl("url").build();

        when(identityVerificationRepository.existsByDocumentNumberAndStatusAndUserIdNot(
                anyString(), anyString(), any())).thenReturn(false);
        when(identityVerificationRepository.findApprovedByNormalizedDocumentNumberExcludingUser(
                anyString(), any())).thenReturn(Optional.empty());
        when(identityVerificationRepository.findPendingByNormalizedDocumentNumberExcludingUser(
                anyString(), any())).thenReturn(Optional.empty());
        when(identityVerificationRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(identityVerificationRepository.save(any())).thenReturn(saved);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        var result = userService.uploadIdentity(userDetails, "url", "PASAPORTE", "AB999999",
                "Test User", LocalDate.of(1990, 1, 1));

        assertNotNull(result);
        assertEquals("PENDING", result.status());
    }

    // --- RefundMethod ---

    @Test
    void deleteRefundMethod_shouldThrow_whenMethodBelongsToOtherUser() {
        UUID methodId = UUID.randomUUID();
        RefundMethod rm = RefundMethod.builder()
                .id(methodId).userId(UUID.randomUUID()).bank("Banco Test").build();
        when(refundMethodRepository.findById(methodId)).thenReturn(Optional.of(rm));

        assertThrows(ResourceNotFoundException.class,
                () -> userService.deleteRefundMethod(userId, methodId));
    }

    @Test
    void deleteRefundMethod_shouldDelete_whenOwner() {
        UUID methodId = UUID.randomUUID();
        RefundMethod rm = RefundMethod.builder()
                .id(methodId).userId(userId).bank("Banco Test").build();
        when(refundMethodRepository.findById(methodId)).thenReturn(Optional.of(rm));

        assertDoesNotThrow(() -> userService.deleteRefundMethod(userId, methodId));
        verify(refundMethodRepository).delete(rm);
    }

    // --- ProfessionalProfile ---

    @Test
    void saveProfessionalProfile_shouldCreateNewProfile_whenNotExists() {
        ProfessionalProfile saved = ProfessionalProfile.builder()
                .id(UUID.randomUUID()).user(user).specialty("Ballet").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(professionalProfileRepository.findByUser_Id(userId)).thenReturn(Optional.empty());
        when(professionalProfileRepository.save(any())).thenReturn(saved);

        Map<String, Object> data = new HashMap<>();
        data.put("specialty", "Ballet");
        data.put("experienceYears", 5);

        var result = userService.saveProfessionalProfile(userId, data);

        assertNotNull(result);
        assertEquals("Ballet", result.getSpecialty());
    }
}
