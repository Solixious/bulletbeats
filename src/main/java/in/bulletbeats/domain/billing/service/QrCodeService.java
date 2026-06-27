package in.bulletbeats.domain.billing.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import in.bulletbeats.config.AppProperties;
import in.bulletbeats.domain.shared.exception.ImageStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class QrCodeService {

    private static final int SIZE = 400;

    private final AppProperties appProperties;

    public String generateAndSave(String content, String filename) {
        try {
            BitMatrix matrix = encode(content);
            Path qrDir = Paths.get(appProperties.getUploadDir(), "qr");
            Files.createDirectories(qrDir);
            MatrixToImageWriter.writeToPath(matrix, "PNG", qrDir.resolve(filename));
            return "qr/" + filename;
        } catch (WriterException | IOException e) {
            throw new ImageStorageException("Failed to generate QR code: " + filename, e);
        }
    }

    public byte[] generateAsBytes(String content) {
        try {
            BitMatrix matrix = encode(content);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new ImageStorageException("Failed to generate QR code bytes", e);
        }
    }

    private BitMatrix encode(String content) throws WriterException {
        return new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, SIZE, SIZE);
    }
}
