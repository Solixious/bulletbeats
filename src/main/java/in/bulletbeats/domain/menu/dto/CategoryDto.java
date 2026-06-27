package in.bulletbeats.domain.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryDto {

    @NotBlank
    @Size(max = 100)
    private String name;

    private String description;

    private int displayOrder;
}
