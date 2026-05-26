package com.modoensayo.shared.controller;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.shared.storage.UnifiedStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileAccessController {

    private final UnifiedStorageService storageService;

    @GetMapping("/{type}/{filename:.+}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Resource> download(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String type,
            @PathVariable String filename) throws MalformedURLException {
        Resource resource = storageService.loadAsResource(type, filename);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
