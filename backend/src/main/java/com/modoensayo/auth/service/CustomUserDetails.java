package com.modoensayo.auth.service;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String password;
    private final String fullName;
    private final boolean enabled;
    private final Set<String> roles;

    public CustomUserDetails(UUID userId, String email, String password, String fullName, boolean enabled, Set<String> roles) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.enabled = enabled;
        this.roles = roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }

    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
}
