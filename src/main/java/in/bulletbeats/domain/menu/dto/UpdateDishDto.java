package in.bulletbeats.domain.menu.dto;

import in.bulletbeats.domain.shared.enums.DishType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateDishDto {

    @NotBlank
    @Size(max = 150)
    private String name;

    private String description;

    private String recipeNotes;

    private Integer prepTimeMinutes;

    @NotNull
    private DishType dishType;

    @Valid
    private List<DishIngredientDto> ingredients;
}
