package in.bulletbeats.domain.billing;

import in.bulletbeats.domain.billing.dto.CafeTableDto;
import in.bulletbeats.domain.billing.service.CafeTableService;
import in.bulletbeats.domain.shared.exception.DuplicateTableException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/tables")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CafeTableController {

    private final CafeTableService cafeTableService;

    @GetMapping
    public String list() {
        return "billing/tables/floor-plan";
    }

    @GetMapping("/list")
    public String listView(Model model) {
        model.addAttribute("tables", cafeTableService.getAllActive());
        return "billing/tables/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("dto", new CafeTableDto());
        model.addAttribute("mode", "create");
        return "billing/tables/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("dto") CafeTableDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("mode", "create");
            return "billing/tables/form";
        }
        try {
            cafeTableService.create(dto);
        } catch (DuplicateTableException e) {
            result.rejectValue("name", "duplicate", e.getMessage());
            model.addAttribute("mode", "create");
            return "billing/tables/form";
        }
        return "redirect:/admin/tables?created";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var table = cafeTableService.getById(id);
        CafeTableDto dto = new CafeTableDto();
        dto.setName(table.getName());
        dto.setCapacity(table.getCapacity());
        model.addAttribute("table", table);
        model.addAttribute("dto", dto);
        model.addAttribute("mode", "edit");
        return "billing/tables/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("dto") CafeTableDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("table", cafeTableService.getById(id));
            model.addAttribute("mode", "edit");
            return "billing/tables/form";
        }
        try {
            cafeTableService.update(id, dto);
        } catch (DuplicateTableException e) {
            result.rejectValue("name", "duplicate", e.getMessage());
            model.addAttribute("table", cafeTableService.getById(id));
            model.addAttribute("mode", "edit");
            return "billing/tables/form";
        }
        return "redirect:/admin/tables?updated";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id) {
        try {
            cafeTableService.deactivate(id);
            return "redirect:/admin/tables?deactivated";
        } catch (IllegalStateException e) {
            return "redirect:/admin/tables?activebills";
        }
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> serveQr(@PathVariable Long id) {
        try {
            byte[] bytes = cafeTableService.getQrBytes(id);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(bytes);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/qr/download")
    public ResponseEntity<byte[]> downloadQr(@PathVariable Long id) {
        try {
            var table = cafeTableService.getById(id);
            byte[] bytes = cafeTableService.getQrBytes(id);
            String filename = "table-" + table.getName().toLowerCase().replace(" ", "-") + "-qr.png";
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(bytes);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/qr/regenerate")
    public String regenerateQr(@PathVariable Long id) {
        cafeTableService.regenerateQrCode(id);
        return "redirect:/admin/tables/" + id + "/edit?qrRegenerated";
    }
}
