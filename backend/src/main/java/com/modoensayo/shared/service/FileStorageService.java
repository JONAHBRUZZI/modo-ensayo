package com.modoensayo.shared.service;

import com.modoensayo.shared.config.StorageConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final StorageConfig storageConfig;

    public String store(MultipartFile file, String subdirectory) throws IOException {
        if ("local".equals(storageConfig.getProvider())) {
            return storeLocal(file, subdirectory);
        }
        return null;
    }

    private String storeLocal(MultipartFile file, String subdirectory) throws IOException {
        Path uploadPath = Paths.get(storageConfig.getUploadDir(), subdirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = UUID.randomUUID() + extension;
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        return "/" + storageConfig.getUploadDir() + "/" + subdirectory + "/" + filename;
    }
}
