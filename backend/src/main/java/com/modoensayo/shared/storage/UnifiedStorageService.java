package com.modoensayo.shared.storage;

import com.modoensayo.shared.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Service
public class UnifiedStorageService implements CloudStorageService {

    private final Path storageDir;
    private final String storageMode;

    public UnifiedStorageService(
            @Value("${app.storage.dir:uploads}") String uploadDir,
            @Value("${app.storage.mode:local}") String storageMode) {
        this.storageDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.storageMode = storageMode;
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new BusinessException("No se pudo crear directorio de storage");
        }
    }

    @Override
    public String upload(MultipartFile file, String type) throws IOException {
        String filename = UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
        Path target = storageDir.resolve(type).resolve(filename);
        Files.createDirectories(target.getParent());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        log.info("File stored [{}] at {}", storageMode, target);

        if ("cloudinary".equalsIgnoreCase(storageMode)) {
            return uploadToCloudinary(target, filename, type);
        }
        return "/api/files/" + type + "/" + filename;
    }

    @Override
    public byte[] download(String fileUrl) throws IOException {
        if ("cloudinary".equalsIgnoreCase(storageMode) || fileUrl.startsWith("http")) {
            throw new BusinessException("Cloudinary files are accessed directly via signed URL");
        }
        String relativePath = fileUrl.replace("/api/files/", "");
        Path filePath = storageDir.resolve(relativePath).normalize();
        if (!filePath.startsWith(storageDir)) {
            throw new BusinessException("Acceso denegado");
        }
        return Files.readAllBytes(filePath);
    }

    @Override
    public void delete(String fileUrl) throws IOException {
        if ("cloudinary".equalsIgnoreCase(storageMode) || fileUrl.startsWith("http")) {
            log.info("Cloudinary deletion skipped for: {}", fileUrl);
            return;
        }
        String relativePath = fileUrl.replace("/api/files/", "");
        Path filePath = storageDir.resolve(relativePath).normalize();
        if (!filePath.startsWith(storageDir)) {
            throw new BusinessException("Acceso denegado");
        }
        Files.deleteIfExists(filePath);
    }

    public Resource loadAsResource(String type, String filename) throws MalformedURLException {
        Path filePath = storageDir.resolve(type).resolve(filename).normalize();
        if (!filePath.startsWith(storageDir)) {
            throw new BusinessException("Acceso denegado");
        }
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        }
        throw new BusinessException("Archivo no encontrado");
    }

    private String uploadToCloudinary(Path localFile, String filename, String type) {
        log.warn("CLOUDINARY MODE: Integracion pendiente. Archivo en local: {}", localFile);
        return "/api/files/" + type + "/" + filename;
    }

    private String sanitize(String name) {
        return name != null ? name.replaceAll("[^a-zA-Z0-9._-]", "_") : "file";
    }
}
