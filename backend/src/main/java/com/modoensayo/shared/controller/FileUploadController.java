package com.modoensayo.shared.controller;

import com.modoensayo.auth.service.CustomUserDetails;
import com.modoensayo.shared.storage.UnifiedStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final UnifiedStorageService storageService;

    @PostMapping
    public ResponseEntity<Map<String, String>> upload(@AuthenticationPrincipal CustomUserDetails user,
                                                       @RequestParam("file") MultipartFile file,
                                                       @RequestParam(defaultValue = "documents") String type) {
        try {
            String url = storageService.upload(file, type);
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("fileUrl", url);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Upload failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Upload failed"));
        }
    }
}
