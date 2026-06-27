package in.bulletbeats.domain.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateComboDto {

    @NotBlank
    @Size(max = 150)
    private String name;

    private String description;

    @NotEmpty
    @Valid
    private List<ComboIngredientDto> ingredients;
}
