package com.modoensayo.shared.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface CloudStorageService {
    String upload(MultipartFile file, String type) throws IOException;
    byte[] download(String fileUrl) throws IOException;
    void delete(String fileUrl) throws IOException;
}
