package com.modoensayo.users.service;

import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import com.modoensayo.reviews.enums.ReviewTargetType;
import com.modoensayo.reviews.repository.ReviewRepository;
import com.modoensayo.users.domain.User;
import com.modoensayo.users.dto.UpdateProfileRequest;
import com.modoensayo.users.dto.UserProfileResponse;
import com.modoensayo.users.repository.UserRoleRepository;
import com.modoensayo.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final ReviewRepository reviewRepository;

    public UserService(UserRepository userRepository,
                       UserRoleRepository userRoleRepository,
                       ReviewRepository reviewRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var roles = userRoleRepository.findByUserId(user.getId()).stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        return new UserProfileResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                roles,
                reviewRepository.findAverageScore(ReviewTargetType.STUDENT, user.getId()),
                reviewRepository.countByTargetTypeAndTargetId(ReviewTargetType.STUDENT, user.getId())
        );
    }

    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        userRepository.save(user);

        return getProfile(email);
    }
}
