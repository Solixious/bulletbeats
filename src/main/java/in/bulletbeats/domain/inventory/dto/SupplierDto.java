package in.bulletbeats.domain.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierDto {

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 20)
    private String phone;
}
