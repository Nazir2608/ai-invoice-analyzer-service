package com.nazir.aiinvoice.infrastructure.storage;

import com.nazir.aiinvoice.domain.strategy.StorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
@Slf4j
public class FileSystemStorageService implements StorageStrategy {

    private static final String UPLOAD_DIR = "uploads";

    @Override
    public String store(MultipartFile file) {
        try {
            File dir = new File(System.getProperty("user.dir") + File.separator + UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(dir.getAbsolutePath() + File.separator + UUID.randomUUID() + "_" + file.getOriginalFilename());
            file.transferTo(dest);
            String filePath = dest.getAbsolutePath();
            log.info("event=file_stored path={}", filePath);
            return filePath;
        } catch (Exception e) {
            log.error("Failed to store file", e);
            throw new RuntimeException("File storage failed", e);
        }
    }
}
