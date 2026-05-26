package com.modoensayo.shared.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private final Path uploadDir = Paths.get("uploads");

    @PostMapping
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                                       @RequestParam(defaultValue = "documents") String type) {
        try {
            Files.createDirectories(uploadDir);
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path target = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            Map<String, String> result = new HashMap<>();
            result.put("url", "/uploads/" + filename);
            result.put("fileUrl", "/uploads/" + filename);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Upload failed"));
        }
    }
}
