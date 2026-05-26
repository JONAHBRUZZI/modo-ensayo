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
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El archivo esta vacio"));
            }
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                return ResponseEntity.badRequest().body(Map.of("error", "Solo se aceptan archivos JPG, PNG o PDF"));
            }
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "El archivo no debe superar los 5MB"));
            }
            String url = storageService.upload(file, type);
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("fileUrl", url);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Upload failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al subir el archivo"));
        }
    }
}
