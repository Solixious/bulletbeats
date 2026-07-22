package in.bulletbeats.domain.billing;

import in.bulletbeats.domain.admin.AppConfigService;
import in.bulletbeats.domain.billing.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/delivery")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DeliveryAdminController {

    private final QrCodeService qrCodeService;
    private final AppConfigService appConfigService;

    @GetMapping
    public String page(Model model) {
        model.addAttribute("orderUrl", orderUrl());
        return "billing/delivery-qr";
    }

    @GetMapping("/qr")
    public ResponseEntity<byte[]> serveQr() {
        byte[] bytes = qrCodeService.generateAsBytes(orderUrl());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(bytes);
    }

    @GetMapping("/qr/download")
    public ResponseEntity<byte[]> downloadQr() {
        byte[] bytes = qrCodeService.generateAsBytes(orderUrl());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"delivery-order-qr.png\"")
                .body(bytes);
    }

    private String orderUrl() {
        return appConfigService.get("app.base-url", "http://localhost:8080") + "/order";
    }
}
