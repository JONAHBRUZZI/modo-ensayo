package com.modoensayo.auth.service;

import com.modoensayo.users.domain.User;
import com.modoensayo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return buildDetails(user);
    }

    public UserDetails loadUserById(UUID id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return null;
        return buildDetails(user);
    }

    private CustomUserDetails buildDetails(User user) {
        Set<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toSet());
        return new CustomUserDetails(user.getId(), user.getEmail(), user.getPasswordHash(),
                user.getFullName(), user.isEnabled(), roles);
    }
}
