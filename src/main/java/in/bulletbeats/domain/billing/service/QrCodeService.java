package in.bulletbeats.domain.billing.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import in.bulletbeats.config.SupabaseStorageService;
import in.bulletbeats.domain.shared.exception.ImageStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class QrCodeService {

    private static final int SIZE = 400;

    private final SupabaseStorageService supabaseStorageService;

    public String generateAndSave(String content, String filename) {
        try {
            byte[] bytes = encodeToBytes(content);
            return supabaseStorageService.upload(bytes, "image/png", "qr/" + filename);
        } catch (WriterException | IOException e) {
            throw new ImageStorageException("Failed to generate QR code: " + filename, e);
        }
    }

    public byte[] generateAsBytes(String content) {
        try {
            return encodeToBytes(content);
        } catch (WriterException | IOException e) {
            throw new ImageStorageException("Failed to generate QR code bytes", e);
        }
    }

    private byte[] encodeToBytes(String content) throws WriterException, IOException {
        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, SIZE, SIZE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }
}
