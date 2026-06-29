package in.bulletbeats.domain.menu.service;

import in.bulletbeats.config.AppProperties;
import in.bulletbeats.config.SupabaseStorageService;
import in.bulletbeats.domain.shared.exception.ImageStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final int THUMBNAIL_MAX_PX = 300;

    private final AppProperties appProperties;
    private final SupabaseStorageService supabaseStorageService;

    public record StoredImage(String imagePath, String thumbnailPath) {}

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

        String objectPath = subDir + "/" + UUID.randomUUID() + "." + extension;
        try {
            return supabaseStorageService.upload(file.getBytes(), file.getContentType(), objectPath);
        } catch (IOException e) {
            throw new ImageStorageException("Failed to read uploaded file", e);
        }
    }

    public StoredImage storeWithThumbnail(MultipartFile file, String subDir) {
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

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ImageStorageException("Failed to read uploaded file", e);
        }

        String objectPath = subDir + "/" + UUID.randomUUID() + "." + extension;
        String imagePath = supabaseStorageService.upload(bytes, file.getContentType(), objectPath);

        String thumbnailPath = null;
        try {
            byte[] thumbBytes = createThumbnail(bytes, extension);
            if (thumbBytes != null) {
                String thumbObjectPath = subDir + "/thumbs/" + UUID.randomUUID() + "." + extension;
                thumbnailPath = supabaseStorageService.upload(thumbBytes, file.getContentType(), thumbObjectPath);
            }
        } catch (Exception ignored) {
            // thumbnail is optional — fall back to full image in templates
        }

        return new StoredImage(imagePath, thumbnailPath);
    }

    public void delete(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        String objectPath = supabaseStorageService.extractObjectPath(imageUrl);
        supabaseStorageService.delete(objectPath);
    }

    private byte[] createThumbnail(byte[] imageBytes, String extension) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (original == null) return null; // format not supported (e.g. webp without plugin)

        int w = original.getWidth();
        int h = original.getHeight();
        if (w <= THUMBNAIL_MAX_PX && h <= THUMBNAIL_MAX_PX) return null; // already small

        double scale = Math.min((double) THUMBNAIL_MAX_PX / w, (double) THUMBNAIL_MAX_PX / h);
        int tw = Math.max(1, (int) (w * scale));
        int th = Math.max(1, (int) (h * scale));

        boolean hasAlpha = extension.equals("png");
        BufferedImage thumb = new BufferedImage(tw, th, hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumb.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(original.getScaledInstance(tw, th, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();

        String format = hasAlpha ? "PNG" : "JPEG";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(thumb, format, out);
        return out.toByteArray();
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new ImageStorageException("File has no extension");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
