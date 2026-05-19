package com.modoensayo.users.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudStorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudStorageService.class);

    public Map<String, String> uploadIdentityDocument(MultipartFile file, String publicId) {
        validateFile(file);
        Map<String, String> result = new HashMap<>();
        result.put("publicId", publicId != null ? publicId : UUID.randomUUID().toString());
        result.put("urlBase", "https://storage.example.com/documents/" + result.get("publicId"));
        return result;
    }

    public String generateSignedUrl(String publicId) {
        return "https://storage.example.com/documents/" + publicId + "?signature=fake-signature";
    }

    public void deleteDocument(String publicId) {
        log.info("Deleting document with publicId: {}", publicId);
    }

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        String contentType = file.getContentType();
        if (!"image/jpeg".equals(contentType)
                && !"image/png".equals(contentType)
                && !"application/pdf".equals(contentType)) {
            throw new IllegalArgumentException("Invalid file type. Allowed: image/jpeg, image/png, application/pdf");
        }

        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }
    }
}
