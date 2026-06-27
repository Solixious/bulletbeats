package in.bulletbeats.domain.admin;

import in.bulletbeats.domain.billing.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/admin/share")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
@RequiredArgsConstructor
public class MenuShareController {

    private final AppConfigService appConfigService;
    private final QrCodeService qrCodeService;

    @GetMapping
    public String sharePage(Model model) {
        String baseUrl = appConfigService.get("app.base-url", "");
        String menuUrl = baseUrl.isEmpty() ? "" : baseUrl + "/public/menu";
        model.addAttribute("menuUrl", menuUrl);
        model.addAttribute("baseUrlConfigured", !baseUrl.isEmpty());
        return "admin/menu-share";
    }

    @GetMapping("/qr.png")
    @ResponseBody
    public ResponseEntity<byte[]> qrImage() {
        String baseUrl = appConfigService.get("app.base-url", "http://localhost:8080");
        String menuUrl = baseUrl + "/public/menu";
        byte[] bytes = qrCodeService.generateAsBytes(menuUrl);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(bytes);
    }
}
