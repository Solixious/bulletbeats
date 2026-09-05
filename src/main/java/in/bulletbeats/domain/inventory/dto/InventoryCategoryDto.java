package in.bulletbeats.domain.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InventoryCategoryDto {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Min(0)
    private int displayOrder;
}
