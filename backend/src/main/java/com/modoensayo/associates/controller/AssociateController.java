package com.modoensayo.associates.controller;

import com.modoensayo.associates.dto.AssociateRequest;
import com.modoensayo.associates.dto.AssociateResponse;
import com.modoensayo.associates.service.AssociateService;
import com.modoensayo.auth.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/associates")
@RequiredArgsConstructor
public class AssociateController {

    private final AssociateService associateService;

    @GetMapping
    public ResponseEntity<List<AssociateResponse>> list(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(associateService.getByOwner(user.getUserId()));
    }

    @PostMapping
    public ResponseEntity<AssociateResponse> create(@AuthenticationPrincipal CustomUserDetails user,
                                                     @RequestBody AssociateRequest req) {
        return ResponseEntity.ok(associateService.create(user.getUserId(), req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails user,
                                        @PathVariable UUID id) {
        associateService.delete(user.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
