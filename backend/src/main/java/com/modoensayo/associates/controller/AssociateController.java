package com.modoensayo.associates.controller;

import com.modoensayo.associates.dto.AssociateRequest;
import com.modoensayo.associates.dto.AssociateResponse;
import com.modoensayo.associates.service.AssociateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/associates")
public class AssociateController {

    private final AssociateService associateService;

    public AssociateController(AssociateService associateService) {
        this.associateService = associateService;
    }

    @PostMapping
    public ResponseEntity<AssociateResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AssociateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(associateService.create(getUserId(userDetails), request));
    }

    @GetMapping
    public ResponseEntity<List<AssociateResponse>> getByOwner(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(associateService.findByOwner(getUserId(userDetails)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id) {
        associateService.delete(getUserId(userDetails), id);
        return ResponseEntity.noContent().build();
    }

    private String getUserId(UserDetails userDetails) {
        return userDetails.getUsername();
    }
}
