package com.modoensayo.shared.service;

import com.modoensayo.shared.config.StorageConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private final StorageConfig config;

    public FileStorageService(StorageConfig config) {
        this.config = config;
    }

    public String store(MultipartFile file, String type) throws IOException {
        String filename = type + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path uploadPath = Paths.get(config.getUploadDir(), filename);
        Files.createDirectories(uploadPath.getParent());
        Files.write(uploadPath, file.getBytes());
        return filename;
    }
}
