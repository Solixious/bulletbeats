package in.bulletbeats.domain.menu.service;

import in.bulletbeats.config.AppProperties;
import in.bulletbeats.domain.shared.exception.ImageStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final AppProperties appProperties;

    public String store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new ImageStorageException("No file provided");
        }
        long maxBytes = (long) appProperties.getMaxImageSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new ImageStorageException("File exceeds maximum size of " + appProperties.getMaxImageSizeMb() + "MB");
        }
        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ImageStorageException("Only jpg, png, and webp files are allowed");
        }

        String filename = UUID.randomUUID() + "." + extension;
        Path targetDir = Paths.get(appProperties.getUploadDir(), subDir);
        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetDir.resolve(filename));
        } catch (IOException e) {
            throw new ImageStorageException("Failed to store image", e);
        }

        return subDir + "/" + filename;
    }

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(appProperties.getUploadDir(), relativePath));
        } catch (IOException ignored) {
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new ImageStorageException("File has no extension");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
